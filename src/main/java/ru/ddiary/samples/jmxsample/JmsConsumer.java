package ru.ddiary.samples.jmxsample;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.test.dao.HibernateSessionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;


public class JmsConsumer implements MessageListener, AutoCloseable {
    private final ActiveMQConnectionFactory _connectionFactory;
    private Connection _connection = null;
    private Session _session = null;
    private MessageConsumer _consumer;
    private String _queueName;
    org.hibernate.Session session = HibernateSessionFactory.getSessionFactory().openSession();

    public JmsConsumer(String url, String queue) {
        _connectionFactory = new ActiveMQConnectionFactory(url);
        _queueName = queue;
    }


    public void init() throws JMSException {

        System.out.println("Init consumer...");
        _connection = _connectionFactory.createConnection();
        _connection.start();
        _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = _session.createQueue(_queueName);
        _consumer = _session.createConsumer(dest);
        _consumer.setMessageListener(this); // ������������� �� ������� onMessage

        System.out.println("Consumer successfully initialized");

    }


    public void onMessage(Message msg) {
        if (msg instanceof TextMessage) {
            try {
                String text = null;
                session.beginTransaction();
                com.test.session.Message message = new com.test.session.Message();
                message.setFirstname(((TextMessage) msg).getText());
                message.setId(msg.getJMSMessageID());
                session.save(message);
                session.getTransaction().commit();
                System.out.println("Received message: " + ((TextMessage) msg).getText());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        } else System.out.println("Received message: " + msg.getClass().getName());
    }


    public void close() throws Exception {
        try {
            if (_session != null) {
                _session.close();
            }

        } catch (JMSException jmsEx) {
            jmsEx.printStackTrace();
        }
        try {
            if (_connection != null)
                _connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
