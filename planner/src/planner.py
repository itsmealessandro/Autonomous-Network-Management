def plan_actions(analysis_result):
    """
    Pianifica azioni basate sui risultati di analisi ricevuti.
    """
    global mqtt_client # DICHIARA DI VOLER USARE LA VARIABILE GLOBALE QUI

    metric = analysis_result.get('metric')
    status = analysis_result.get('status')
    current_value = analysis_result.get('value')

    if not all([metric, status, current_value is not None]):
        logger.warning(f"Risultati di analisi incompleti o malformati: {analysis_result}")
        return

    planned_action = None
    action_details = ACTIONS.get(metric, {})

    if status == 'CRITICAL':
        planned_action = action_details.get('CRITICAL')
    elif status == 'WARNING':
        planned_action = action_details.get('WARNING')

    if planned_action:
        # --- INIZIO MODIFICA QUI ---
        # Vecchio payload:
        # payload = json.dumps({
        #     'action': planned_action,
        #     'metric': metric,
        #     'value': current_value,
        #     'status': status,
        #     'timestamp': datetime.utcnow().isoformat()
        # })
        #
        # Nuovo payload: solo la stringa del comando
        payload = planned_action
        # --- FINE MODIFICA QUI ---


        # Il topic deve essere specifico per l'attuatore che deve ricevere il comando
        # Dobbiamo derivarlo dalla metrica, proprio come fa l'attuatore nel suo COMMANDS_TOPIC
        # Esempio: "Network/bandwidth_usage/commands"
        # Quindi, se la metrica è "bandwidth", il topic diventa "Network/bandwidth/commands"
        # Questo richiede che i nomi delle metriche nel Planner corrispondano ai nomi usati negli attuatori.

        # Correggi il topic di destinazione basandoti sul GENERAL_TOPIC dell'attuatore Java:
        # L'attuatore Bandwidth in Java usa "Network/bandwidth_usage/commands".
        # Il Planner usa "bandwidth". Dobbiamo fare una mappatura o standardizzare i nomi.
        # Per ora, supponiamo una mappatura semplice:
        target_topic_mapping = {
            "bandwidth": "Network/bandwidth_usage/commands",
            "latency": "Network/latency/commands", 
            "packet_loss": "Network/packet_loss/commands", 
            "suspicious_activity": "Network/suspicious_activity/commands",
            "traffic_flow": "Network/traffic_flow/commands"
        }
        action_topic = target_topic_mapping.get(metric)

        if action_topic:
            try:
                if mqtt_client:
                    mqtt_client.publish(action_topic, payload)
                    logger.info(f"Azione pianificata e pubblicata: Metrica='{metric}', Azione='{planned_action}', Topic='{action_topic}'")
                else:
                    logger.error("Errore: mqtt_client non è stato inizializzato. Impossibile pubblicare l'azione.")
            except Exception as mqtt_pub_err:
                logger.error(f"Errore durante la pubblicazione dell'azione per '{metric}': {mqtt_pub_err}", exc_info=True)
        else:
            logger.error(f"Nessun topic di destinazione definito per la metrica '{metric}'.")
    else:
        logger.info(f"Nessuna azione necessaria o definita per metrica '{metric}' con stato '{status}'.")