(ns io.github.michalmela.cogni-java-credentials
  (:require
   [cognitect.aws.credentials :as c])
  (:import
   (software.amazon.awssdk.auth.credentials AwsCredentials AwsCredentialsProvider AwsSessionCredentials DefaultCredentialsProvider)))

(defn- aws-sdk-java-v2-credentials->cognitect-credentials
  "Converts AWS Java SDK v2 credentials to cognitect/aws-api credentials map."
  [^AwsCredentials sdk-creds]
  (when sdk-creds
    {:aws/access-key-id             (.accessKeyId sdk-creds)
     :aws/secret-access-key         (.secretAccessKey sdk-creds)
     :aws/session-token             (when (instance? AwsSessionCredentials sdk-creds) (.sessionToken ^AwsSessionCredentials sdk-creds))
     :cognitect.aws.credentials/ttl (when (instance? AwsSessionCredentials sdk-creds)
                                      (cognitect.aws.credentials/calculate-ttl
                                       {:Expiration (some-> ^AwsSessionCredentials sdk-creds
                                                            .expirationTime
                                                            (.orElse nil))}))}))

(defrecord AwsSdkV2CredentialsProvider [^AwsCredentialsProvider java-provider]
  c/CredentialsProvider
  (fetch [_]
    (aws-sdk-java-v2-credentials->cognitect-credentials
     (.resolveCredentials java-provider))))

(defn as-cognitect-provider
  "Returns a cognitect CredentialsProvider instance that uses the AWS SDK for Java's AwsCredentialProvider
  (DefaultCredentialsProviderChain by default) to resolve credentials."
  ([]
   (as-cognitect-provider (DefaultCredentialsProvider/create)))
  ([^AwsCredentialsProvider sdk-creds-provider]
   (c/cached-credentials-with-auto-refresh (->AwsSdkV2CredentialsProvider sdk-creds-provider))))
