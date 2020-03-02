set -eux

#
# Install docker client
#
pacman --sync --refresh --noconfirm docker

#
# Install AWS CLI
#
pacman --sync --refresh --noconfirm aws-cli

# Cleanup
rm -rf /var/cache/pacman/pkg/*
