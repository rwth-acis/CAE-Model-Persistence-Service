#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi
NODE_ID_SEED=${NODE_ID_SEED:-$RANDOM}
EXTRA_ETH_WAIT=${EXTRA_ETH_WAIT:-30}
CONFIG_ENDPOINT_WAIT=${CONFIG_ENDPOINT_WAIT:-21600}
ETH_PROPS_DIR=/src/etc/
ETH_PROPS=i5.las2peer.registry.data.RegistryConfiguration.properties
function waitForEndpoint {
    /src/wait-for-command/wait-for-command.sh -c "nc -z ${1} ${2:-80}" --time ${3:-10} --quiet
}

function host { echo ${1%%:*}; }
function port { echo ${1#*:}; }
# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.modelPersistenceService.ModelPersistenceService.properties'
export WEB_CONNECTOR_PROPERTY_FILE='etc/i5.las2peer.connectors.webConnector.WebConnector.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}
export CREATE_DB_SQL='database/ModelPersistenceService_Database.sql'
export CREATE_WIREFRAME_SQL='database/Wireframe_Extension.sql'
export CREATE_METADATA_SQL='database/Metadata_Extension.sql'
export MYSQL_DATABASE='commedit'

# check mandatory variables
[[ -z "${MYSQL_USER}" ]] && \
    echo "Mandatory variable MYSQL_USER is not set. Add -e MYSQL_USER=myuser to your arguments." && exit 1
[[ -z "${MYSQL_PASSWORD}" ]] && \
    echo "Mandatory variable MYSQL_PASSWORD is not set. Add -e MYSQL_PASSWORD=mypasswd to your arguments." && exit 1

# set defaults for optional service parameters
[[ -z "${SERVICE_PASSPHRASE}" ]] && export SERVICE_PASSPHRASE='Passphrase'
[[ -z "${MYSQL_HOST}" ]] && export MYSQL_HOST='mysql'
[[ -z "${MYSQL_PORT}" ]] && export MYSQL_PORT='3306'
[[ -z "${SEMANTIC_CHECK_SERVICE}" ]] && export SEMANTIC_CHECK_SERVICE=''
[[ -z "${CODE_GENERATION_SERVICE}" ]] && export CODE_GENERATION_SERVICE='i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1'
[[ -z "${METADATA_SERVICE}" ]] && export METADATA_SERVICE='i5.las2peer.services.metadataService.MetadataService@0.1'
[[ -z "${DEPLOYMENT_URL}" ]] && export DEPLOYMENT_URL="http://localhost:${HTTP_PORT}"

# set defaults for optional web connector parameters
[[ -z "${START_HTTP}" ]] && export START_HTTP='TRUE'
[[ -z "${START_HTTPS}" ]] && export START_HTTPS='FALSE'
[[ -z "${SSL_KEYSTORE}" ]] && export SSL_KEYSTORE=''
[[ -z "${SSL_KEY_PASSWORD}" ]] && export SSL_KEY_PASSWORD=''
[[ -z "${CROSS_ORIGIN_RESOURCE_DOMAIN}" ]] && export CROSS_ORIGIN_RESOURCE_DOMAIN='*'
[[ -z "${CROSS_ORIGIN_RESOURCE_MAX_AGE}" ]] && export CROSS_ORIGIN_RESOURCE_MAX_AGE='60'
[[ -z "${ENABLE_CROSS_ORIGIN_RESOURCE_SHARING}" ]] && export ENABLE_CROSS_ORIGIN_RESOURCE_SHARING='TRUE'
[[ -z "${OIDC_PROVIDERS}" ]] && export OIDC_PROVIDERS='https://api.learning-layers.eu/o/oauth2,https://accounts.google.com'

# configure service properties

function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}
set_in_service_config jdbcUrl "jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/"
set_in_service_config jdbcSchema ${MYSQL_DATABASE}
set_in_service_config jdbcLogin ${MYSQL_USER}
set_in_service_config jdbcPass ${MYSQL_PASSWORD}
set_in_service_config semanticCheckService ${SEMANTIC_CHECK_SERVICE}
set_in_service_config codeGenerationService ${CODE_GENERATION_SERVICE}
set_in_service_config metadataService ${METADATA_SERVICE}
set_in_service_config deploymentUrl ${DEPLOYMENT_URL}
set_in_service_config clusterHelmWrapperUrl ${CLUSTER_HELM_WRAPPER_URL}
set_in_service_config clusterHelmRegistryUrl ${CLUSTER_HELM_REGISTRY_URL}

# configure web connector properties

function set_in_web_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${WEB_CONNECTOR_PROPERTY_FILE}
}
set_in_web_config httpPort ${HTTP_PORT}
set_in_web_config httpsPort ${HTTPS_PORT}
set_in_web_config startHttp ${START_HTTP}
set_in_web_config startHttps ${START_HTTPS}
set_in_web_config sslKeystore ${SSL_KEYSTORE}
set_in_web_config sslKeyPassword ${SSL_KEY_PASSWORD}
set_in_web_config crossOriginResourceDomain "${CROSS_ORIGIN_RESOURCE_DOMAIN}"
set_in_web_config crossOriginResourceMaxAge ${CROSS_ORIGIN_RESOURCE_MAX_AGE}
set_in_web_config enableCrossOriginResourceSharing ${ENABLE_CROSS_ORIGIN_RESOURCE_SHARING}
set_in_web_config oidcProviders ${OIDC_PROVIDERS}

# ensure the database is ready
while ! mysqladmin ping -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} --silent; do
    echo "Waiting for mysql at ${MYSQL_HOST}:${MYSQL_PORT}..."
    sleep 1
done
echo "${MYSQL_HOST}:${MYSQL_PORT} is available. Continuing..."

# Create the database on first run
if ! mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "desc ${MYSQL_DATABASE}.model" > /dev/null 2>&1; then
    echo "Creating database schema..."
    mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} < ${CREATE_DB_SQL}
fi

# insert wireframe schema extension into the database
if [[ ! -z "${INIT_WIREFRAME_EXTENSION}" ]]; then
    if ! mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "desc ${MYSQL_DATABASE}.Wireframe" > /dev/null 2>&1; then
        echo "Adding wireframe extension to the database schema..."
        mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} < ${CREATE_WIREFRAME_SQL}
    fi
fi

# insert metadata schema extension into the database
if ! mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "desc ${MYSQL_DATABASE}.MetadataDoc" > /dev/null 2>&1; then
    echo "Adding metadata extension to the database schema..."
    mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ${MYSQL_DATABASE} < ${CREATE_METADATA_SQL}
fi

if [ -n "$LAS2PEER_CONFIG_ENDPOINT" ]; then
    echo Attempting to autoconfigure registry blockchain parameters ...
    if waitForEndpoint $(host ${LAS2PEER_CONFIG_ENDPOINT}) $(port ${LAS2PEER_CONFIG_ENDPOINT}) $CONFIG_ENDPOINT_WAIT; then
        echo "Port is available (but that may just be the Docker daemon)."
        echo Downloading ...
        wget --quiet --tries=inf "http://${LAS2PEER_CONFIG_ENDPOINT}/${ETH_PROPS}" -O "${ETH_PROPS_DIR}${ETH_PROPS}"
        echo done.
    else
        echo Registry configuration endpoint specified but not accessible. Aborting.
        exit 1
    fi
fi

if [ -n "$LAS2PEER_BOOTSTRAP" ]; then
    if waitForEndpoint $(host ${LAS2PEER_BOOTSTRAP}) $(port ${LAS2PEER_BOOTSTRAP}) 600; then
        echo Las2peer bootstrap available, continuing.
    else
        echo Las2peer bootstrap specified but not accessible. Aborting.
        exit 3
    fi
fi

# it's realistic for different nodes to use different accounts (i.e., to have
# different node operators). this function echos the N-th mnemonic if the
# variable WALLET is set to N. If not, first mnemonic is used
function selectMnemonic {
    declare -a mnemonics=("differ employ cook sport clinic wedding melody column pave stuff oak price" "memory wrist half aunt shrug elbow upper anxiety maximum valve finish stay" "alert sword real code safe divorce firm detect donate cupboard forward other" "pair stem change april else stage resource accident will divert voyage lawn" "lamp elbow happy never cake very weird mix episode either chimney episode" "cool pioneer toe kiwi decline receive stamp write boy border check retire" "obvious lady prize shrimp taste position abstract promote market wink silver proof" "tired office manage bird scheme gorilla siren food abandon mansion field caution" "resemble cattle regret priority hen six century hungry rice grape patch family" "access crazy can job volume utility dial position shaft stadium soccer seven")
    if [[ ${WALLET} =~ ^[0-9]+$ && ${WALLET} -lt ${#mnemonics[@]} ]]; then
    # get N-th mnemonic
        echo "${mnemonics[${WALLET}]}"
    else
        # note: zsh and others use 1-based indexing. this requires bash
        echo "${mnemonics[0]}"
    fi
}

#prepare pastry properties
echo external_address = $(curl -s https://ipinfo.io/ip):${LAS2PEER_PORT} > etc/pastry.properties

echo Starting las2peer node ...
if [ -n "$LAS2PEER_ETH_HOST" ]; then
    echo ... using ethereum boot procedure: 
    java $(echo $ADDITIONAL_JAVA_ARGS) \
        -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher \
        --service-directory service \
        --port $LAS2PEER_PORT \
        $([ -n "$LAS2PEER_BOOTSTRAP" ] && echo "--bootstrap $LAS2PEER_BOOTSTRAP") \
        --node-id-seed $NODE_ID_SEED \
        --ethereum-mnemonic "$(selectMnemonic)" \
        $(echo $ADDITIONAL_LAUNCHER_ARGS) \
        startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) \
        startWebConnector \
        "node=getNodeAsEthereumNode()" "registry=node.getRegistryClient()" "n=getNodeAsEthereumNode()" "r=n.getRegistryClient()" \
        $(echo $ADDITIONAL_PROMPT_CMDS) \
        interactive
else
    echo ... using non-ethereum boot procedure:
    java $(echo $ADDITIONAL_JAVA_ARGS) \
        -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher \
        --service-directory service \
        --port $LAS2PEER_PORT \
        $([ -n "$LAS2PEER_BOOTSTRAP" ] && echo "--bootstrap $LAS2PEER_BOOTSTRAP") \
        --node-id-seed $NODE_ID_SEED \
        $(echo $ADDITIONAL_LAUNCHER_ARGS) \
        startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) \
        startWebConnector \
        $(echo $ADDITIONAL_PROMPT_CMDS) \
        interactive
fi
