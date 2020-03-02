set -eux

# https://docs.docker.com/install/linux/docker-ce/ubuntu/

#
# Docker client pre-reqs
#
apt-get update
apt-get install -y              \
    apt-transport-https         \
    ca-certificates             \
    curl                        \
    software-properties-common  \
    gnupg2

cat /etc/os-release
  distro=`cat /etc/os-release | grep '^ID='               | sed -e 's/ID=//g'               -e 's/"//g'`
codename=`cat /etc/os-release | grep '^VERSION_CODENAME=' | sed -e 's/VERSION_CODENAME=//g' -e 's/"//g'`
if [ -z "$codename" ]; then 
  codename=`cat /etc/os-release | grep VERSION= | sed 's/["()]//g' | cut -d' ' -f2`
fi
curl -fsSL https://download.docker.com/linux/${distro}/gpg | apt-key add -
add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/${distro} ${codename} stable"
apt-get update

#
# Install docker client
#
apt-get install -y docker-ce-cli

#
# Install AWS CLI
#
apt-get install -y    \
  python3             \
  python3-setuptools  \
  python3-dev         \
  python3-pip
pip3 --no-cache-dir install awscli

# Cleanup
rm -rf /var/lib/apt/lists/*
