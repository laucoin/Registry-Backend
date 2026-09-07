#!/usr/bin/env sh
# Gives every user seeded by blueprints/registry.yaml the shared AU_DEV_PASSWORD.
#
# This is a script and not another blueprint entry because Authentik blueprints cannot set
# passwords: the user serializer exposes name, email, type, attributes — but no password
# field, and the importer has no special case for one. Run it after the first apply.
set -eu

cd "$(dirname "$0")/.."  # local-dev/, where compose.yml and .env live

docker compose exec -T registry-authentik-server ak shell -c '
import os

from authentik.core.models import User

password = os.environ["AU_DEV_PASSWORD"]
usernames = [
    "administrator",
    "coordinator",
    "participant",
    "blocked-user",
    "blocked-profile",
    "unverified",
]

for username in usernames:
    user = User.objects.filter(username=username).first()
    if user is None:
        print(f"!! {username} does not exist yet — apply the blueprint first")
        continue
    user.set_password(password)
    user.save()
    print(f"ok {username}")
'
