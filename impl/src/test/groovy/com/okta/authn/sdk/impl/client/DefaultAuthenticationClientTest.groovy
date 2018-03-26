package com.okta.authn.sdk.impl.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.okta.authn.sdk.AuthenticationFailureException
import com.okta.authn.sdk.AuthenticationStateHandler
import com.okta.authn.sdk.CredentialsException
import com.okta.authn.sdk.FactorValidationException
import com.okta.authn.sdk.InvalidAuthenticationStateException
import com.okta.authn.sdk.InvalidRecoveryAnswerException
import com.okta.authn.sdk.InvalidTokenException
import com.okta.authn.sdk.InvalidUserException
import com.okta.authn.sdk.resource.ActivatePassCodeFactorRequest
import com.okta.authn.sdk.resource.AuthenticationResponse
import com.okta.authn.sdk.resource.AuthenticationStatus
import com.okta.authn.sdk.resource.VerifyPassCodeFactorRequest
import com.okta.sdk.client.AuthenticationScheme
import com.okta.sdk.impl.cache.DisabledCacheManager
import com.okta.sdk.impl.http.MediaType
import com.okta.sdk.impl.http.Request
import com.okta.sdk.impl.http.RequestExecutor
import com.okta.sdk.impl.http.authc.DefaultRequestAuthenticatorFactory
import com.okta.sdk.impl.http.support.DefaultResponse
import com.okta.sdk.impl.util.DefaultBaseUrlResolver
import com.okta.sdk.resource.ResourceException
import com.okta.sdk.resource.user.factor.CallFactorProfile
import com.okta.sdk.resource.user.factor.FactorProvider
import com.okta.sdk.resource.user.factor.FactorType
import com.spotify.hamcrest.jackson.IsJsonObject
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.mockito.Mockito
import org.testng.annotations.Test

import static org.hamcrest.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyZeroInteractions

import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.endsWith

import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject

class DefaultAuthenticationClientTest {

    @Test
    void authenticationSuccess() {

        def client = createClient("authenticationSuccess")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()

        requestExecutor.requestMatchers.add(bodyMatches(
            jsonObject()
                .where("username", is(jsonText("username1")))
                .where("password", is(jsonText("password2")))
        ))

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleSuccess(response)
    }

    @Test
    void changePasswordTest() {
        def client = createClient("changePasswordTest")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()

        requestExecutor.requestMatchers.add(bodyMatches(
            jsonObject()
                .where("stateToken",  is(jsonText("stateToken1")))
                .where("oldPassword", is(jsonText("oldPassword1")))
                .where("newPassword", is(jsonText("newPassword2")))
        ))

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.changePassword("oldPassword1".toCharArray(), "newPassword2".toCharArray(), "stateToken1", stateHandler)
        verify(stateHandler).handleSuccess(response)
    }

    @Test
    void resetPasswordTest() {
        def client = createClient("resetPasswordTest")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()

        requestExecutor.requestMatchers.add(bodyMatches(
                jsonObject()
                   .where("stateToken",  is(jsonText("stateToken1")))
                   .where("newPassword", is(jsonText("newPassword2")))
        ))

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.resetPassword("newPassword2".toCharArray(), "stateToken1", stateHandler)
        verify(stateHandler).handleSuccess(response)
    }

    @Test
    void enrollFactorTest() {
        def client = createClient("enrollFactorTest")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()

        requestExecutor.requestMatchers.add(
            bodyMatches(
                    jsonObject()
                        .where("stateToken",  is(jsonText("stateToken1")))
                        .where("factorType", is(jsonText("sms")))
                        .where("provider", is(jsonText("OKTA")))
                        .where("profile", is(jsonObject()
                            .where("phoneNumber", is(jsonText("555-555-1212")))))
        ))

        def factorProfile = client.instantiate(CallFactorProfile)
                                        .setPhoneNumber("555-555-1212")

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.enrollFactor(FactorType.SMS, FactorProvider.OKTA, factorProfile, "stateToken1", stateHandler)
        verify(stateHandler).handleMfaEnrollActivate(response)
    }

    @Test
    void activateFactorTest() {
        def client = createClient("activateFactorTest")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()
        String factorId = "factor321"

        requestExecutor.requestMatchers.add(bodyMatches(
            jsonObject()
                .where("stateToken", is(jsonText("stateToken1")))
                .where("passCode", is(jsonText("123456")))
        ))

        requestExecutor.requestMatchers.add(
            urlMatches(
                endsWith("/api/v1/authn/factors/${factorId}/lifecycle/activate")
        ))

        def request = client.instantiate(ActivatePassCodeFactorRequest)
                                        .setPassCode("123456")
                                        .setStateToken("stateToken1")

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.activateFactor(factorId, request, stateHandler)
        verify(stateHandler).handleSuccess(response)
    }

    @Test
    void verifyFactorTest() {
        def client = createClient("verifyFactorTest")
        StubRequestExecutor requestExecutor = client.getRequestExecutor()
        String factorId = "factor321"

        requestExecutor.requestMatchers.add(bodyMatches(
            jsonObject()
                .where("stateToken", is(jsonText("stateToken1")))
                .where("passCode", is(jsonText("123456")))
        ))

        requestExecutor.requestMatchers.add(
            urlMatches(
                endsWith("/api/v1/authn/factors/${factorId}/verify")
        ))

        def request = client.instantiate(VerifyPassCodeFactorRequest)
                                        .setPassCode("123456")
                                        .setStateToken("stateToken1")

        def stateHandler = mock(AuthenticationStateHandler)
        AuthenticationResponse response = client.verifyFactor(factorId, request, stateHandler)
        verify(stateHandler).handleMfaChallenge(response)
    }

    @Test
    void eachStatusTest() {

        def client = createClient("eachStatusTest")
        def stateHandler = mock(AuthenticationStateHandler)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.SUCCESS)
        AuthenticationResponse response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleSuccess(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.UNAUTHENTICATED)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleUnauthenticated(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.PASSWORD_WARN)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handlePasswordWarning(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.PASSWORD_EXPIRED)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handlePasswordExpired(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.RECOVERY)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleRecovery(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.RECOVERY_CHALLENGE)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleRecoveryChallenge(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.PASSWORD_RESET)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handlePasswordReset(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.LOCKED_OUT)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleLockedOut(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.MFA_ENROLL)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleMfaEnroll(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.MFA_ENROLL_ACTIVATE)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleMfaEnrollActivate(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.MFA_REQUIRED)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleMfaRequired(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.MFA_CHALLENGE)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleMfaChallenge(response)

        client.getRequestExecutor().interpolationData.put("status", AuthenticationStatus.UNKNOWN)
        response = client.authenticate("username1", "password2".toCharArray(), null, stateHandler)
        verify(stateHandler).handleUnknown(response)
    }

    @Test
    void eachErrorCodeTest() {

        def client = createClient("eachErrorCodeTest")
        verifyExceptionThrown(client, 401, AuthenticationFailureException.ERROR_CODE, AuthenticationFailureException)
        verifyExceptionThrown(client, 403, CredentialsException.ERROR_CODE, CredentialsException)
        verifyExceptionThrown(client, 403, FactorValidationException.ERROR_CODE, FactorValidationException)
        verifyExceptionThrown(client, 403, InvalidAuthenticationStateException.ERROR_CODE, InvalidAuthenticationStateException)
        verifyExceptionThrown(client, 403, InvalidRecoveryAnswerException.ERROR_CODE, InvalidRecoveryAnswerException)
        verifyExceptionThrown(client, 401, InvalidTokenException.ERROR_CODE, InvalidTokenException)
        verifyExceptionThrown(client, 403, InvalidUserException.ERROR_CODE, InvalidUserException)

        // other error
        verifyExceptionThrown(client, 444, "other-code", ResourceException)
    }

    def verifyExceptionThrown(def client, int httpStatus, String errorCode, Class<? extends Exception> exception) {

        def stateHandler = mock(AuthenticationStateHandler)
        def requestExecutor = mock(RequestExecutor)
        client.setRequestExecutor(requestExecutor)

        def responseText = """
            {
                "errorCode": "${errorCode}",
                "errorSummary": "Some error message",
                "errorLink": "a-link",
                "errorId": "en-error-id",
                "errorCauses": []
            }
        """

        TestUtil.expectException(exception) {
            def response = new DefaultResponse(httpStatus, MediaType.APPLICATION_JSON, new ByteArrayInputStream(responseText.bytes), responseText.length())
            when(requestExecutor.executeRequest(Mockito.any(Request))).thenReturn(response)
            client.authenticate("wrong-username", "or-password".toCharArray(), null, stateHandler)
        }
        verifyZeroInteractions(stateHandler)
    }

    WrappedAuthenticationClient createClient(callingTestMethodName = Thread.currentThread().getStackTrace()[6].methodName) {
        def baseUrlResolver = new DefaultBaseUrlResolver("http://${getClass().name}/${callingTestMethodName}")
        def proxy = null
        def cacheManager = new DisabledCacheManager()
        def authScheme = AuthenticationScheme.NONE
        def requestAuthenticatorFactory = new DefaultRequestAuthenticatorFactory()
        return new WrappedAuthenticationClient(baseUrlResolver, proxy, cacheManager, authScheme, requestAuthenticatorFactory,  1)
    }


    // matchers
    // TODO: move out to own class
    static Matcher<Request> bodyMatches(final IsJsonObject matcher) {
        return new TypeSafeMatcher<Request>() {

            @Override
            protected boolean matchesSafely(Request item) {
                JsonNode json = new ObjectMapper().readTree(item.body.text)
                return matcher.matches(json)
            }

            @Override
            void describeTo(Description description) {
                description.appendText("body failed to match ")
                matcher.describeTo(description)
            }
        }
    }

    static Matcher<Request> urlMatches(final Matcher<String> matcher) {
        return new TypeSafeMatcher<Request>() {

            @Override
            protected boolean matchesSafely(Request item) {
                return matcher.matches(item.resourceUrl.toString())
            }

            @Override
            void describeTo(Description description) {
                description.appendText("Request URL failed to match ")
                matcher.describeTo(description)
            }

            @Override
            protected void describeMismatchSafely(Request item, Description mismatchDescription) {
                matcher.describeMismatch(item.resourceUrl.toString(), mismatchDescription)
            }
        }
    }
}


