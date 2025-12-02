#!/bin/bash

ACTION="$1"         # block | unblock
PORTS="$2"          # exemplo: "12,15"
DURATION="$3"       # usado somente para block

JAVA="/usr/bin/java"
APP_HOME="/home/matheus-freitas/Documentos/UDESC/OGMR/Trabalho-Final-OGMR/network-manager"
CLASSPATH="$APP_HOME/target/*"
CLASS="com.org.snmp.PortManager"

LOG="$APP_HOME/portmanager.log"

run_now() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') Executando: $ACTION $PORTS" >> "$LOG"
    $JAVA -cp "$CLASSPATH" $CLASS "$ACTION" "$PORTS" >> "$LOG" 2>&1
}

if [ "$ACTION" = "unblock" ]; then
    run_now
    exit 0
fi

# block + cron

if [ "$ACTION" = "block" ]; then

    run_now   # bloqueia agora

    # calcula horário de desbloqueio
    UNBLOCK_TIME=$(date -d "+$DURATION seconds" "+%M %H %d %m")
    read MIN HOUR DAY MONTH <<< "$UNBLOCK_TIME"

    # comando a ser executado pelo cron
    CRON_CMD="$JAVA -cp $CLASSPATH $CLASS unblock $PORTS >> $LOG 2>&1"

    # instala cron
    (
        echo "SHELL=/bin/bash"
        echo "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        crontab -l 2>/dev/null
        echo "$MIN $HOUR $DAY $MONTH * $CRON_CMD"
    ) | crontab -

    echo "Desbloqueio agendado para $DAY/$MONTH às $HOUR:$MIN" >> "$LOG"
fi
