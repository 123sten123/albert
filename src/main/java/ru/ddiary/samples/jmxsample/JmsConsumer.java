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

/**
 * Класс получателя сообщений
 * Логика работы событийная:
 * после инициализации класс подписывается на событие onMessage,
 * которое возникает при получении целевым объектом сообщения.
 * Данный класс реализует интерфейс MessageListener, что делает
 * возможным передавать его экзепляры в метод _consumer.setMessageListener(...);
 *
 */
public class JmsConsumer implements MessageListener, AutoCloseable {
    private final ActiveMQConnectionFactory _connectionFactory;
    private Connection _connection = null;
    private Session _session = null;
    private MessageConsumer _consumer;
    private String _queueName;
    org.hibernate.Session session = HibernateSessionFactory.getSessionFactory().openSession();

    /**
     * Конструктор используется в случае, когда брокер не требует авторизации.
     * Здесь я не стал добавлять вариант с авторизацией. Он показан в producer-е.
     * Брокер ActiveMQ из коробки настроен на работу без авторизации.
     */
    public JmsConsumer(String url, String queue) {
        _connectionFactory = new ActiveMQConnectionFactory(url);
        _queueName = queue;
    }

    /**
     * Инициализация consumer-а.
     * Обратите внимание на добавление экземпляра этого класса
     * в качестве подписчика на событие получения сообщений.
     */
    public void init() throws JMSException {

        System.out.println("Init consumer...");
        _connection = _connectionFactory.createConnection();
        _connection.start();
        _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = _session.createQueue(_queueName);
        _consumer = _session.createConsumer(dest);
        _consumer.setMessageListener(this); // подписываемся на событие onMessage

        System.out.println("Consumer successfully initialized");

    }

    /**
     * Обработчик события появления сообщения в целевом объекте.
     * Этот метод является частью реализации интерфейса MessageListener.
     */
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

    /**
     * Метод закрывает созединения перед разрушением объекта.
     * Этот метод является реализацией интерфейса Autoclosable,
     * добавленого в Java7 и используемого в блоке try-with-resources.
     */
    public void close() throws Exception {
        try {
            if (_session != null){
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
