package com.hmdp;

import com.hmdp.entity.User;
import com.hmdp.service.IUserService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisIdWorker;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;




import javax.annotation.Resource;
import java.io.BufferedWriter;

import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ErrorManager;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private RedissonClient redissonClient;
    private RLock redisson_lock;
    @BeforeEach
            void setUp(){
        redisson_lock = redissonClient.getLock("yuan");
    }


    @Test
    void method1(){
        boolean islock=redisson_lock.tryLock();
        if(!islock){
            System.out.println("faield");
            return;
        }try{
            System.out.println("success");
            method2();
        }finally {
            redisson_lock.unlock();
        }
    }

    private void method2() {
        boolean islock=redisson_lock.tryLock();
        if(!islock){
            System.out.println("22faield");
            return;
        }try{
            System.out.println("22success");
        }finally {
            redisson_lock.unlock();
        }
    }


    @Test
    public void genJwt(){
        Map<String,Object> claims = new HashMap<>();
        claims.put("id",1);
        claims.put("username","Tom");
        String jwt = Jwts.builder()
                .setClaims(claims) //自定义内容(载荷)
                .signWith(SignatureAlgorithm.HS256, "itheima") //签名算法
                .setExpiration(new Date(System.currentTimeMillis() +
                        24*3600*1000)) //有效期
                .compact();
        System.out.println(jwt);
    }
    @Resource//默认安照名称进行装配，和@autowired一样实现自动装配，但@aotowired默认安照类型进行装配
    private RedisIdWorker redisIdWorker;
    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }


    @Test
    void testRedisson() throws Exception{
        //获取锁(可重入)，指定锁的名称
        RLock lock = redissonClient.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean isLock = lock.tryLock(1,10, TimeUnit.SECONDS);
        //判断获取锁成功
        if(isLock){
            try{
                System.out.println("执行业务");
            }finally{
                //释放锁
                lock.unlock();
            }

        }



    }
    @Autowired
    private IUserService userService;
    @Test
    public void function(){
        String loginUrl = "http://localhost:8081/user/login"; // 替换为实际的登录URL
        String tokenFilePath = "tokens.txt"; // 存储Token的文件路径

        try {

            HttpClient httpClient = HttpClients.createDefault();

            BufferedWriter writer = new BufferedWriter(new FileWriter(tokenFilePath));

            // 从数据库中获取用户手机号
            List<User> users = userService.list();

            for(User user : users) {
                String phoneNumber = user.getPhone();

                // 构建登录请求
                HttpPost httpPost = new HttpPost(loginUrl);
                //（1.如果作为请求参数传递）
                //List<NameValuePair> params = new ArrayList<>();
                //params.add(new BasicNameValuePair("phone", phoneNumber));
                // 如果登录需要提供密码，也可以添加密码参数
                // params.add(new BasicNameValuePair("password", "user_password"));
                //httpPost.setEntity(new UrlEncodedFormEntity(params));
                // (2.如果作为请求体传递)构建请求体JSON对象
                JSONObject jsonRequest = new JSONObject();
                jsonRequest.put("phone", phoneNumber);
                jsonRequest.put("code", "123331");
                StringEntity requestEntity = new StringEntity(
                        jsonRequest.toString(),
                        ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

                // 发送登录请求
                HttpResponse response = httpClient.execute(httpPost);
                // 处理登录响应，获取token
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity);
                    System.out.println(responseString);
                    // 解析响应，获取token，这里假设响应是JSON格式的
                    // 根据实际情况使用合适的JSON库进行解析
                    String token = parseTokenFromJson(responseString);
                    System.out.println("手机号 " + phoneNumber + " 登录成功，Token: " + token);
                    // 将token写入txt文件
                    writer.write(token);
                    writer.newLine();
                } else {
                    System.out.println("手机号 " + phoneNumber + " 登录失败");
                }
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 解析JSON响应获取token的方法，这里只是示例，具体实现需要根据实际响应格式进行解析
    private static String parseTokenFromJson(String json) {
        try {
            // 将JSON字符串转换为JSONObject
            JSONObject jsonObject = new JSONObject(json);
            // 从JSONObject中获取名为"token"的字段的值
            String token = jsonObject.getString("data");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // 解析失败，返回null或者抛出异常，具体根据实际需求处理
        }
    }

}
