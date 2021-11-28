####### 
https://katacoda.com/courses/kubernetes/launch-single-node-cluster

##
apt update && \
    apt-get install openjdk-11-jdk software-properties-common -y

##
curl -O https://raw.githubusercontent.com/spinnaker/halyard/master/install/debian/InstallHalyard.sh && \
 chmod +x ./InstallHalyard.sh && \
 useradd -m zhang && \
 bash InstallHalyard.sh --user zhang -y

   ### cat /var/log/spinnaker/halyard/halyard.log

JAVA_HOME=$(dirname $( readlink -f $(which java) ))
JAVA_HOME=$(realpath "$JAVA_HOME"/../)
export JAVA_HOME

##
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl && \
    kubectl version --client

chmod 777 /root/ && chmod 777 /root/.kube/config

##
MINIO_ROOT_USER=$(< /dev/urandom tr -dc a-z | head -c${1:-4})
MINIO_ROOT_PASSWORD=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c${1:-8})
MINIO_PORT="9010"

# Start the container
docker run -it -d --rm -v ~/.minio-data/:/data --name minio-4-spinnaker -p ${MINIO_PORT}:${MINIO_PORT} \
 -e MINIO_ROOT_USER=${MINIO_ROOT_USER} -e  MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD} \
 minio/minio  server /data --address :${MINIO_PORT}

echo "
MINIO_ROOT_USER=${MINIO_ROOT_USER}
MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
ENDPOINT=http://$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' minio-4-spinnaker):${MINIO_PORT} "

## hal version list
hal config version edit --version 1.26.6

hal config provider kubernetes enable
hal config provider kubernetes account add my-k8s \
           --provider-version v2 \
           --context $(kubectl config current-context)
hal config deploy edit --type=distributed --account-name my-k8s


DEPLOYMENT="default"
mkdir -p ~/.hal/$DEPLOYMENT/profiles/
echo spinnaker.s3.versioning: false > ~/.hal/$DEPLOYMENT/profiles/front50-local.yml

echo ${MINIO_ROOT_PASSWORD} | hal config storage s3 edit --endpoint $ENDPOINT \
    --access-key-id ${MINIO_ROOT_USER} \
    --secret-access-key
    
hal config storage edit --type s3
hal config storage s3 edit --path-style-access=true 

  ## hal config storage edit --type redis

hal deploy apply 
  ##hal deploy apply --no-validate
