# Okta Java Authentication SDK Migration Guide
 
This SDK uses semantic versioning and follows Okta's [library version policy](https://developer.okta.com/code/library-versions/). In short, we do not make breaking changes unless the major version changes!

## Migrating from 1.x.x to 2.0.0

Version 2.0.0 of this SDK introduces a number of breaking changes from previous versions. 
In addition to se new classes/interfaces, some existing classes/interfaces are no longer backward compatible due to method renaming and signature changes.

### Package `com.okta.authn.sdk.resource`

- Replaced `com.okta.sdk.resource.user.factor.FactorProfile` interface with `com.okta.authn.sdk.resource.FactorProfile` interface.
- Replaced `com.okta.sdk.resource.user.factor.FactorProvider` interface with `com.okta.authn.sdk.resource.FactorProvider` interface.
- Replaced `com.okta.sdk.resource.user.factor.FactorType` interface with `com.okta.authn.sdk.resource.FactorType` interface.

Note that the old interfaces above were pulled in from **okta-sdk-java** earlier. 
These are now migrated and would reside locally within Authentication SDK. 

- Added below interfaces that are extensions of `com.okta.authn.sdk.resource.FactorProfile` interface:
  ```java
  - CallFactorProfile
  - EmailFactorProfile
  - HardwareFactorProfile
  - PushFactorProfile
  - SecurityQuestionFactorProfile
  - SmsFactorProfile
  - TokenFactorProfile
  - TotpFactorProfile
  - U2fFactorProfile
  - WebAuthnFactorProfile
  - WebFactorProfile
  ```
 
Below SDK classes/interfaces are **deprecated** and will be removed from this project.

```
- com.okta.sdk.http.UserAgentProvider
- com.okta.authn.sdk.impl.http.AuthnSdkUserAgentProvider
- com.okta.sdk.cache.CacheManager
- com.okta.sdk.client.AuthenticationScheme
- com.okta.sdk.impl.http.authc.RequestAuthenticatorFactory
```

Below SDK classes were previously moved to [okta-commons-java](https://github.com/okta/okta-commons-java)).

```
- com.okta.sdk.client.Proxy
- com.okta.sdk.lang.Classes
- com.okta.sdk.lang.Assert
- com.okta.sdk.lang.Strings
- com.okta.sdk.lang.Collections
- com.okta.sdk.lang.Locales
- com.okta.sdk.impl.http.MediaType
- com.okta.sdk.impl.http.Request
- com.okta.sdk.impl.http.RequestExecutor
- com.okta.sdk.impl.http.support.DefaultResponse
- com.okta.sdk.impl.util.BaseUrlResolver
- com.okta.sdk.impl.http.Response
```