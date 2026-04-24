package com.acme.nuxeo.auth;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.BasicAuthenticator;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class BasicSudoAuthenticator extends BasicAuthenticator {

    public static final String BASIC_SUDO_AUTH_PLUGIN = "BASIC_SUDO_AUTH";
    public static final String SUDO_HEADER = "X-NXsudo";
    public static final String PARAM_ALLOWED_GROUP = "AllowedGroup";

    protected String allowedGroup;

    private static final Logger log = LogManager.getLogger(BasicSudoAuthenticator.class);

    @Override
    public void initPlugin(Map<String, String> parameters) {
        super.initPlugin(parameters);
        if (parameters == null) {
            return;
        }
        allowedGroup = parameters.getOrDefault(PARAM_ALLOWED_GROUP, SecurityConstants.ADMINISTRATORS);
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest req, HttpServletResponse resp) {
        // Step 1: retrieve admin credentials using BasicAuthenticator logic
        UserIdentificationInfo base = super.handleRetrieveIdentity(req, resp);
        if (base == null || base.getUserName() == null) {
            return base;
        }
        /*
        // Optional: only allow sudo on REST paths
        String uri = req.getRequestURI();
        if (onlyPathPrefix != null && !onlyPathPrefix.isBlank() && (uri == null || !uri.startsWith(onlyPathPrefix))) {
            return base;
        }
         */
        // Step 2: check for impersonation header
        String target = req.getHeader(SUDO_HEADER);
        if (target == null || target.isBlank()) {
            log.debug("No {} header found in the request", SUDO_HEADER);
            // Let the chain handle the failure (typically prompts/401)
            return null;
        }
        String targetUser = target.trim();
        // Step 3: validate caller credentials and authorization
        UserManager um = Framework.getService(UserManager.class);
        // Validate admin password NOW (because we will not login as admin)
        boolean ok = um.checkUsernamePassword(base.getUserName(), base.getPassword());
        if (!ok) {
            // Let the chain handle the failure (typically prompts/401)
            return null;
        }
        NuxeoPrincipal caller = Framework.doPrivileged(() -> um.getPrincipal(base.getUserName()));
        if (caller == null) {
            log.debug("Unknow user [{}]", base.getUserName());
            //TODO fail authentication
            return null;
        }
        boolean isAllowed = caller.isAdministrator() || caller.isMemberOf(allowedGroup);
        if (!isAllowed) {
            // ignore sudo header if not allowed (or return null to hard-fail)
            log.debug("Impersonation not allowed for [{}]", caller);
            //TODO fail authentication
            return null;
        }
        NuxeoPrincipal targetPrincipal = Framework.doPrivileged(() -> um.getPrincipal(targetUser));
        if (targetPrincipal == null) {
            // target user does not exist -> ignore or fail
            log.debug("Uknown target user [{}]", target);
            //TODO fail authentication
            return null;
        }
        // Step 4: return the target identity as "already checked"
        UserIdentificationInfo sudo = new UserIdentificationInfo(target);
        sudo.setAuthPluginName(BASIC_SUDO_AUTH_PLUGIN);
        sudo.setCredentialsChecked(true); // indicates authentication already performed [7](https://doc.nuxeo.com/javadoc/2025-lts/org/nuxeo/ecm/platform/api/login/UserIdentificationInfo.html)
        // Optional: keep trace of original caller (for your own logging usage)
        sudo.setToken("sudoBy:" + base.getUserName());

        return sudo;
    }

}
