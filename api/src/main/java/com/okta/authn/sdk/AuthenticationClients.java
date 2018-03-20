/*
 * Copyright 2014 Stormpath, Inc.
 * Modifications Copyright 2018 Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.authn.sdk;

import com.okta.sdk.lang.Classes;

/**
 * Static utility/helper class for working with {@link AuthenticationClient} resources. For example:
 * <pre>
 * <b>AuthenticationClients.builder()</b>
 *     // ... etc ...
 *     .setProxy(new Proxy("192.168.2.120", 9001))
 *     .build();
 * </pre>
 *
 * <p>See the {@link AuthenticationClientBuilder AuthenticationClientBuilder} JavaDoc for extensive documentation on client configuration.</p>
 *
 * @see AuthenticationClientBuilder
 * @since 0.1.0
 */
public final class AuthenticationClients {

    /**
     * Returns a new {@link AuthenticationClientBuilder} instance, used to construct {@link AuthenticationClient} instances.
     *
     * @return a a new {@link AuthenticationClientBuilder} instance, used to construct {@link AuthenticationClient} instances.
     */
    public static AuthenticationClientBuilder builder() {
        return (AuthenticationClientBuilder) Classes.newInstance("com.okta.authn.sdk.impl.DefaultClientBuilder");
    }

}
