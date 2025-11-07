# cogni-java-credentials

![Clojars Version](https://img.shields.io/clojars/v/io.github.michalmela%2Fcogni-java-credentials)


Bridges [AWS Java SDK v2](https://github.com/aws/aws-sdk-java-v2) credentials with [cognitect/aws-api](https://github.com/cognitect-labs/aws-api).

It enables `cognitect/aws-api` to use the AWS Java SDK v2's
`DefaultCredentialsProvider` (or any other `AwsCredentialsProvider` implementation, if so desired), which:
 * supports more authentication schemes than cognitect/aws-api includes ATOW:
    * [SSO](https://github.com/cognitect-labs/aws-api/issues/182)
    * [Web Identity Token](https://github.com/cognitect-labs/aws-api/issues/133)
    * [Container Credentials and Pod Identities](https://github.com/cognitect-labs/aws-api/issues/286)
 * allows to take advantage of AWS' support for the Java SDK v2 provider chain
 * reduces SRE headache in a polyglot ecosystem due to the provider chain consistency across other AWS SDKs,
   e.g. for [Python](https://boto3.amazonaws.com/v1/documentation/api/latest/guide/credentials.html#configuring-credentials)
   or [Go](https://docs.aws.amazon.com/sdk-for-go/v2/developer-guide/configure-gosdk.html#specifying-credentials)

## Usage

### Dependencies

The library pulls in optional dependencies by default:

 * software.amazon.awssdk/sso
 * software.amazon.awssdk/ssooidc
 * software.amazon.awssdk/eksauth

- which provides convenient versatility, but they can be excluded if absolutely not needed.

#### leiningen

```clojure
:dependencies [[io.github.michalmela/cogni-java-credentials "..."]]
```

#### deps.edn

```clojure
io.github.michalmela/cogni-java-credentials {:mvn/version "..."}
```

### REPL example

```clojure
(require '[cognitect.aws.client.api :as aws])
(require '[io.github.michalmela.cogni-java-credentials :as java-credentials])

(aws/invoke
 (aws/client {:api                  :sts
              :region               (or (System/getenv "AWS_REGION")
                                        (System/getProperty "aws.region")
                                        "eu-west-1")
              :credentials-provider (java-credentials/as-cognitect-provider)})
 {:op :GetCallerIdentity})
```
