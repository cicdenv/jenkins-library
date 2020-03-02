set -eux

# https://docs.docker.com/install/linux/docker-ce/centos/

#
# Docker client pre-reqs
#
yum install -y yum-utils
yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo

#
# Install docker client
#
yum install -y docker-ce-cli

#
# Install AWS CLI
#
yum install -y epel-release
yum update -y
yum install -y python36 python36-devel python36-pip
pip3 install --upgrade pip
pip3 install --no-cache-dir awscli

# Cleanup
yum clean all
