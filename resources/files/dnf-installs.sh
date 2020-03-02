set -eux

# https://docs.docker.com/install/linux/docker-ce/fedora/

#
# Docker client pre-reqs
#
dnf -y install dnf-plugins-core
dnf config-manager \
    --add-repo \
    https://download.docker.com/linux/fedora/docker-ce.repo

#
# Install docker client
#
dnf -y install docker-ce-cli

#
# Install AWS CLI
#
dnf -y install awscli

# Cleanup
dnf clean all
