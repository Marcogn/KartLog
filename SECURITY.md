# Security Policy

KartLog is a single-user, mostly offline Android app maintained by one
person in their spare time. There is no dedicated security team and no
SLA, but reports are taken seriously and looked at as soon as possible.

## Reporting a vulnerability

Please **do not** open a public issue for security vulnerabilities.

Instead, use GitHub's private reporting flow:
[Report a vulnerability](../../security/advisories/new) (Security tab →
"Report a vulnerability"). This opens a private advisory visible only to
the maintainer until a fix is ready.

If you can't use that flow, open a regular issue asking for an alternative
contact without including any vulnerability details.

## Scope

Things worth reporting: anything that could leak locally-stored collectible
progress or race results, or allow arbitrary code execution via a crafted
backup import file (JSON export/import of the user state, SPEC §4).

Things generally out of scope: issues that require a rooted/compromised
device. The app's only network access at runtime is downloading game images
(characters, outfits, cups, rallies) over HTTPS from the Super Mario Wiki
CDN (`mario.wiki.gallery`), at URLs fixed in the bundled `seed/` data; no
user data is ever sent. The rest of the network access in this project
happens at build time, in the `tools/seedgen` data-extraction script.

## Supported versions

Only the latest published release is supported; older releases don't
receive backported fixes. See the
[Releases](../../releases) page for the current version.
