# Aggiungi queste funzioni e modifiche

# Dopo le importazioni
from datetime import datetime

# Soglie di allarme (personalizzabili via env)
THRESHOLDS = {
    "bandwidth": {"WARNING": 70, "CRITICAL": 90},
    "latency": {"WARNING": 100, "CRITICAL": 200},
    "packet_loss": {"WARNING": 5, "CRITICAL": 10},
    "suspicious_activity": {"WARNING": 10, "CRITICAL": 20}
}

# Funzione di analisi
def analyze_metric(metric, value):
    """
    Analizza un valore per un certo metric e restituisce un dizionario con informazioni sull'analisi.
    
    Il dizionario contiene le seguenti chiavi:
    - metric: il nome del metric
    - value: il valore del metric
    - timestamp: la data e l'ora dell'analisi
    - status: lo stato dell'analisi (NORMAL, WARNING, CRITICAL, UNKNOWN, ERROR)
    - recommendation: una raccomandazione per l'operatore (optional)
    - related_metrics: una lista di metric correlati (optional)
    """
    analysis = {"metric": metric, "value": value, "timestamp": datetime.utcnow().isoformat()}
    
    try:
        threshold = THRESHOLDS.get(metric)
        if not threshold:
            analysis["status"] = "UNKNOWN"
            return analysis
        
        if value >= threshold["CRITICAL"]:
            analysis["status"] = "CRITICAL"
            analysis["recommendation"] = f"Immediate action required for {metric}"
        elif value >= threshold["WARNING"]:
            analysis["status"] = "WARNING"
            analysis["recommendation"] = f"Monitor {metric} closely"
        else:
            analysis["status"] = "NORMAL"
            
        # Logica aggiuntiva basata su correlazioni
        if metric == "latency" and value > 150:
            analysis["related_metrics"] = ["bandwidth", "packet_loss"]
            
    except Exception as e:
        logger.error(f"Analysis error for {metric}: {str(e)}")
        analysis["status"] = "ERROR"
    
    return analysis
# Modifica la callback MQTT
def on_message(client, userdata, msg):
    """
    Callback MQTT per l'elaborazione dei messaggi pubblicati da sensori e collector.

    Riceve un payload JSON contenente un valore numerico e lo elabora come segue:
    1. Salva il valore su InfluxDB come punto di dati nella bucket "network_metrics"
    2. Esegue l'analisi del valore con la funzione `analyze_metric`
    3. Pubblica il risultato dell'analisi sul topic "analysis/results" come JSON
    """
    try:
        payload = msg.payload.decode('utf-8')
        topic_parts = msg.topic.split('/')
        source = topic_parts[0]
        metric = topic_parts[-1]
        
        # Estrazione valore con gestione errori migliorata
        try:
            data = json.loads(payload)
            value = data.get('value', float('nan'))
        except:
            try:
                value = float(payload)
            except:
                value = float('nan')
        
        if not isinstance(value, (int, float)):
            logger.warning(f"Invalid value for {metric}: {value}")
            return
        
        # 1. Salva su InfluxDB
        point = Point("network_metrics") \
            .tag("source", source) \
            .tag("metric", metric) \
            .field("value", value) \
            .time(time.time_ns(), WritePrecision.NS)
            
        write_api.write(
            bucket=required_env_vars['INFLUX_BUCKET'],
            record=point
        )
        
        # 2. Esegui analisi
        analysis = analyze_metric(metric, value)
        
        # 3. Pubblica risultati analisi
        client.publish("analysis/results", json.dumps(analysis))
        logger.info(f"Published analysis: {metric}={value} => {analysis['status']}")
        
    except Exception as e:
        logger.error(f"Message processing error: {str(e)}", exc_info=True)