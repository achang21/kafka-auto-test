package demo.producer;

import demo.model.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private final KafkaTemplate<String,Object> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String,Object> kafkaTemplate){
        this.kafkaTemplate=kafkaTemplate;
    }

    public void send(OrderEvent event){
        kafkaTemplate.send("order-created",event);
    }
}
