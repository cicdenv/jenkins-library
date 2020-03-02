set -eux

# Needs curl, tar
if command -v curl; then
    #
    # Install docker client
    #
    curl -sL "https://download.docker.com/linux/static/stable/x86_64/docker-19.03.2.tgz" \
    | tar -xz -C /tmp/download
    mv /tmp/download/docker/docker /usr/local/bin

    #
    # Install AWS CLI
    # https://docs.aws.amazon.com/cli/latest/userguide/install-bundle.html
    #
    command -v python \
    || curl -sL "https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh" \
    | /bin/bash

    curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "/tmp/awscli-bundle.zip"
    (cd /tmp; unzip awscli-bundle.zip)
    /tmp/awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws

    # Cleanup
    rm -rf /tmp/download /tmp/awscli-bundle.zip
fi
