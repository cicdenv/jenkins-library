set -eux

# https://docs.docker.com/install/linux/docker-ce/fedora/

#
# Docker client pre-reqs
#
apk --no-cache add curl

#
# Install docker client
#
apk --no-cache add docker

#
# Install AWS CLI
#
apk --no-cache add  python3
python3 -m ensurepip
pip3 install --no-cache-dir --upgrade pip setuptools
pip3 install --no-cache-dir awscli

# Cleanup
rm -rf /root/.cache
rm -rf /var/cache/apk/*
