#!/bin/bash
# Parse les arguments de la ligne de commande
DEBUG=0
# Charger les variables d'environnement depuis le fichier .env s'il existe
if [ -f ".env" ]; then
  echo "Chargement des variables depuis le fichier .env"
  while IFS= read -r line || [[ -n "$line" ]]; do
    # Ignorer les commentaires et les lignes vides
    if [[ ! "$line" =~ ^# && -n "$line" ]]; then
      # Supprimer les espaces de début et de fin
      line=$(echo "$line" | xargs)
      # Exporter la variable
      export "$line"
    fi
  done < ".env"
fi

for arg in "$@"; do
  case $arg in
    debug=1)
      DEBUG=1
      shift
      ;;
  esac
done

# Définition des variables d'environnement obligatoires avec leurs messages d'erreur associés
# Format: "VARIABLE_NAME:Message d'erreur à afficher si manquant"
declare -a REQUIRED_ENV_VARS=(
  "DATABASE_URL:La variable d'environnement DATABASE_URL n'est pas définie. Usage: DATABASE_URL='mysql://user:pass@host:port/dbname?serverVersion=x.x' $0"
  "ADP_API_HOST:La variable d'environnement ADP_API_HOST n'est pas définie. Elle doit correspondre au domaine (sans le /api)"
  "ADP_API_USERNAME:La variable d'environnement ADP_API_USERNAME n'est pas définie."
  "ADP_API_PASSWORD:La variable d'environnement ADP_API_PASSWORD n'est pas définie."
  # Ajoutez facilement d'autres variables ici sous le même format
  # "API_KEY:La clé API est requise pour accéder aux services externes"
  # "LOG_LEVEL:Le niveau de journalisation doit être défini (DEBUG, INFO, WARN, ERROR)"
)

# Fonction pour vérifier si une variable d'environnement est définie
check_env_var() {
  local var_name="${1%%:*}"
  local error_message="${1#*:}"
  
  if [ -z "${!var_name}" ]; then
    return 1
  fi
  return 0
}

# Vérification de toutes les variables d'environnement requises
MISSING_VARS=0
MISSING_VARS_LIST=""

for env_var in "${REQUIRED_ENV_VARS[@]}"; do
  var_name="${env_var%%:*}"
  error_message="${env_var#*:}"
  
  if ! check_env_var "$env_var"; then
    MISSING_VARS=$((MISSING_VARS + 1))
    MISSING_VARS_LIST="${MISSING_VARS_LIST}Erreur: ${error_message}\n"
  fi
done

# Si des variables sont manquantes, afficher les erreurs et quitter
if [ $MISSING_VARS -gt 0 ]; then
  echo -e "Les variables d'environnement suivantes sont manquantes :"
  echo -e "${MISSING_VARS_LIST}"
  exit 1
fi

echo "Les variables d'environnement DATABASE_URL, ADP_API_HOST, ADP_API_USERNAME et ADP_API_PASSWORD ont été détectées."

# Continuez avec le traitement de DATABASE_URL
# Copier la valeur pour éviter de modifier la variable d'origine
DB_URL="${DATABASE_URL}"
# Supprimer le préfixe éventuel (comme "mysql")
DB_URL=$(echo "${DB_URL}" | sed -E 's/^[a-zA-Z]+:\/\//\/\//g')
# Extraire les différentes parties
# Username et password sont entre // et @
USER_PASS=$(echo "${DB_URL}" | sed -E 's/^\/\/([^@]+)@.*/\1/')
USERNAME=$(echo "${USER_PASS}" | cut -d':' -f1)
PASSWORD=$(echo "${USER_PASS}" | cut -d':' -f2)
# Hostname et port sont entre @ et /
HOST_PORT=$(echo "${DB_URL}" | sed -E 's/^\/\/[^@]+@([^\/]+).*/\1/')
HOSTNAME=$(echo "${HOST_PORT}" | cut -d':' -f1)
PORT=$(echo "${HOST_PORT}" | cut -d':' -f2)
# Nom de la base de données est entre / et ?
DATABASE=$(echo "${DB_URL}" | sed -E 's/^.*\/([^?]+)\?.*/\1/')

# Afficher les résultats seulement en mode debug
if [ $DEBUG -eq 1 ]; then
  echo "Username: ${USERNAME}"
  echo "Password: ${PASSWORD}"
  echo "Hostname: ${HOSTNAME}"
  echo "Port: ${PORT}"
  echo "Database Name: ${DATABASE}"
fi

# Exporter les variables pour qu'elles soient accessibles au programme Java
export DB_USERNAME="${USERNAME}"
export DB_PASSWORD="${PASSWORD}"
export DB_HOSTNAME="${HOSTNAME}"
export DB_PORT="${PORT}"
export DB_NAME="${DATABASE}"

# Afficher les variables exportées seulement en mode debug
if [ $DEBUG -eq 1 ]; then
  echo ""
  echo "Variables exportées:"
  echo "DB_USERNAME=${DB_USERNAME}"
  echo "DB_PASSWORD=${DB_PASSWORD}"
  echo "DB_HOSTNAME=${DB_HOSTNAME}"
  echo "DB_PORT=${DB_PORT}"
  echo "DB_NAME=${DB_NAME}"
fi

cd /Users/hachimhassani/projects/etljobs/sql2json2api/usecasesConfig/am
java -Dspring.profiles.active=sql2json2api -jar ../../target/sql2json2api-0.0.1-SNAPSHOT.jar