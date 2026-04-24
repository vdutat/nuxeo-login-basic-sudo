# nuxeo-login-basic-sudo
## About / Synopsis

This plugin demonstrates how to define a custom authentication plugin name `BASIC_SUDO_AUTH` allowing admin users (or member of a group defined in the authentication plugin's configuration) to impersonate another user while quering the **Nuxeo REST API** and **automation** endpoint by using a HTTP header `X-NXsudo`.

The following XML contribute, which you need to define in your Nuxeo project, adds the authentication plugin `BASIC_SUDO_AUTH` to the authentication chain of the **Nuxeo REST API** and **automation** endpoint:
```
<component name="com.acme.nuxeo.auth.basic-sudo.chain.config">
  <require>org.nuxeo.ecm.platform.login.mocksaml.auth</require>
  <require>org.nuxeo.ecm.restapi.server.auth.config</require>

  <extension
      target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
      point="specificChains">

    <specificAuthenticationChain name="Automation">
        <urlPatterns>
            <url>(.*)/automation.*</url>
        </urlPatterns>

        <replacementChain>
        <!-- Put sudo plugin BEFORE AUTOMATION_BASIC_AUTH so it gets a chance first -->
            <plugin>BASIC_SUDO_AUTH</plugin>
        <!-- Keep the rest of the default chain -->
            <plugin>AUTOMATION_BASIC_AUTH</plugin>
        </replacementChain>
    </specificAuthenticationChain>

  </extension>

  <extension
      target="org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService"
      point="specificChains">

    <specificAuthenticationChain name="RestAPI">
        <urlPatterns>
            <url>(.*)/api/v.*</url>
        </urlPatterns>

        <replacementChain>
        <!-- Put sudo plugin BEFORE AUTOMATION_BASIC_AUTH so it gets a chance first -->
            <plugin>BASIC_SUDO_AUTH</plugin>
        <!-- Keep the rest of the default chain -->
            <plugin>AUTOMATION_BASIC_AUTH</plugin>
            <plugin>TOKEN_AUTH</plugin>
            <plugin>OAUTH2_AUTH</plugin>
            <plugin>JWT_AUTH</plugin>
        </replacementChain>
    </specificAuthenticationChain>

  </extension>

</component>
```

Here is a `curl` command that allows the `Administrator` to impersonate user `user1` in order to update a document using the **Nuxeo REST API**:
```
curl -XPUT -su Administrator:Administrator \
-H "Content-Type:application/json" -H "properties:*" \
-H 'X-NXsudo:user1' \
http://localhost:8080/nuxeo/api/v1/path/default-domain/workspaces/ws1/pdf1.pdf \
-d '{
  "entity-type": "document",
  "properties": {
    "dc:description": "Description...",
    "dc:source": "Source...",
    "dc:expired": "2036-04-17T08:00:00.000Z"
  }, 
  "context":{}
}'
```

It was generated with the following commands:
```
mkdir nuxeo-login-basic-sudo && cd $_
nuxeo b multi-module contribution
# Edit contribution's XML file
nuxeo b package
mvn clean install
```

## Table of contents

> * [nuxeo-login-basic-sudo](#nuxeo-login-basic-sudo)
>   * [About / Synopsis](#about--synopsis)
>   * [Table of contents](#table-of-contents)
>   * [Installation](#installation)
>   * [Requirements](#requirements)
>   * [Build](#build)
>   * [License](#license)
>   * [About Hyland Nuxeo](#about-hyland-nuxeo)

## Requirements

Building requires the following software:

* git
* maven

## Build

```
git clone ...
cd nuxeo-login-basic-sudo

mvn clean install
```

## Installation

```
nuxeoctl mp-install nuxeo-login-basic-sudo/nuxeo-login-basic-sudo-package/target/nuxeo-login-basic-sudo-*.zip
```

## Support

**These features are not part of the Nuxeo Production platform, they are not supported**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

## About Hyland Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).


