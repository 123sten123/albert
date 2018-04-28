package ru.ddiary.samples.jmxsample;

import com.test.dao.HibernateSessionFactory;
import com.test.session.Message;
import org.hibernate.Query;
import org.hibernate.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс приложения.
 * Проект используется конструкцию try-with-resources
 * поэтому может быть собран для java не ниже 7 версии.
 */
final public class Program {
    /**
     * Entry point приложения.
     */
    public static synchronized void main(String[] args) {
        Program program = new Program();
        program.run();
    }


    public synchronized void run() {
        String url = "tcp://localhost:61616"; // url коннектора брокера
        Session session = HibernateSessionFactory.getSessionFactory().openSession();
        session.beginTransaction();


        try (JmsProducer producer = new JmsProducer(url);
             JmsConsumer consumer = new JmsConsumer(url, " test.in")) {
            consumer.init();
            producer.start();


            BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while (!(line = rdr.readLine()).equalsIgnoreCase("stop")) // для выхода нужно набрать в консоли stop
            {
                producer.send(line);
            }


            consumer.close();
            System.out.println("\nЧтение записей таблицы");
            String query = "select firstname from Message";

            List<Object> list = (ArrayList<Object>) session.createQuery(query).list();
//            List<String> list = (ArrayList<String>) session.createQuery(query).list();
            for (int i = 0; i < list.size(); i++) {
                producer.send(list.get(i).toString());
//                producer._messagesQueue.add(list.get(i));
                TimeUnit.MILLISECONDS.sleep(10);


            }
            System.out.println("Bye!");


        } catch (Throwable e) {
            e.printStackTrace();
        }
        String stringQuery = "DELETE FROM Message";
        Query query2 = session.createQuery(stringQuery);
        query2.executeUpdate();
        session.getTransaction().commit();

    }
}
