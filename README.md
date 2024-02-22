# gerrit-gitlab-oauth

Little custom OAuth plugin for Gerrit.

On top of standard authentication, it can

- look up user's registered e-mail addresses in a GitLab account and try to match them against the `email-domain` from the config. On a successful match user is authenticated, and the matching e-mail becomes an e-mail used within a Gerrit instance.
- verify user's membership in a specified project or a group. If both are configured then user must be a member of both. Inherited membership from supergroups also counts.

## Build

Java 17 minimum is required.

```sh
# Should create a plugin JAR in target/
$ mvn package
```

## Installation

https://gerrit-review.googlesource.com/Documentation/config-plugins.html#installation

## Configuration

This plugin demands `read_user` scope in order to read users' registered e-mails.

```
[auth]
	type = OAUTH
[plugin "gitlab-oauth"]
	# Required
	oauth-client-id = "<CLIENT_ID>"
	# Required
	oauth-client-secret = "<CLIENT_SECRET>"
	# Optional
	# E-mail domain whitelist
	# Upon successful authentication, e-mail that matches the whitelist
	# will be chosen as a user's e-mail in Gerrit
	email-domain = "example.com"
	# Optional
	# Private token which has enough permissions to check the group
	# or project membership of a user
	org-private-token = "<PRIVATE_TOKEN"
	# Optional
	# Project ID, Noop if `org-private-token` is not provisioned
	project-membership = 12345
	# Optional
	# Noop if `org-private-token` is not provisioned
	group-membership = 54321
```
