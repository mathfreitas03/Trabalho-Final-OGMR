#!/bin/bash

# Uso: ./schedule_ports.sh <block|unblock> <portas> <duracao_segundos>

ACTION="$1"
PORTS="$2"
DURATION="$3"

JAR_PATH="/opt/network-manager/network-manager.jar"

CLASS="com.org.snmp.PortManager"

# ---------------

if [[ -z "$ACTION" || -z "$PORTS" ]]; then
  echo "Uso: $0 <block|unblock> <porta1,porta2,...> [duracao_em_segundos]"
  exit 1
fi

# ---------------- BLOCK ----------------
if [[ "$ACTION" == "block" ]]; then

  echo "Bloqueando portas: $PORTS"

  java -cp "$JAR_PATH" $CLASS block "$PORTS"

  if [[ -z "$DURATION" ]]; then
    echo "Nenhuma duração informada — NÃO será agendado desbloqueio."
    exit 0
  fi

  # Agenda desbloqueio
  RUN_AT=$(date -d "now + $DURATION seconds" "+%M %H %d %m %u")

  CRON_CMD="java -cp $JAR_PATH $CLASS unblock $PORTS"

  # Remove a própria entrada após executar
  FINAL_CMD="$CRON_CMD; crontab -l | grep -v \"$CRON_CMD\" | crontab -"

  ( crontab -l 2>/dev/null; echo "$RUN_AT $FINAL_CMD" ) | crontab -

  echo "Desbloqueio agendado para daqui a $DURATION segundos."

# ---------------- UNBLOCK ----------------
elif [[ "$ACTION" == "unblock" ]]; then

  echo "Desbloqueando portas: $PORTS"

  java -cp "$JAR_PATH" $CLASS unblock "$PORTS"

else
  echo "Ação inválida: $ACTION"
  exit 2
fi
