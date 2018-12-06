package com.fangjia.sjdbc.datasource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@SpringBootConfiguration
// 扫描 Mapper 接口并容器管理
@MapperScan(basePackages = DataSourceConfig.PACKAGE, sqlSessionFactoryRef = "clusterSqlSessionFactory")
public class DataSourceConfig {

    static final String PACKAGE = "com.fangjia.sjdbc.repository";
    static final String MAPPER_LOCATION = "classpath:META-INF/mappers/*.xml";
    public static DataBaseMessage masterDataBaseMessage;
    public static DataBaseMessage dashBoardDataBaseMessage;

    @Value("${datasource.url}")
    private String url;

    @Value("${datasource.username}")
    private String user;

    @Value("${datasource.password}")
    private String password;

    @Value("${datasource.driverClassName}")
    private String driverClass;

    @Value("${dashBoard.datasource.url}")
    private String dashBoardUrl;

    @Value("${dashBoard.datasource.username}")
    private String dashBoardUser;

    @Value("${dashBoard.datasource.password}")
    private String dashBoardPassword;

    @Value("${dashBoard.datasource.driverClassName}")
    private String dashBoardDriverClass;

    @Bean(name = "dashBoardDatabaseMessage")
    public DataBaseMessage dashBoardDatabaseMessage() {
        DataBaseMessage boardDataBaseMessage = new DataBaseMessage();
        boardDataBaseMessage.setUrl(dashBoardUrl);
        boardDataBaseMessage.setUserName(dashBoardUser);
        boardDataBaseMessage.setPassword(dashBoardPassword);
        dashBoardDataBaseMessage = boardDataBaseMessage;
        return boardDataBaseMessage;
    }

    @Bean(name = "masterDatabaseMessage")
    public DataBaseMessage masterDatabaseMessage() {
        DataBaseMessage dataBaseMessage = new DataBaseMessage();
        dataBaseMessage.setUrl(url);
        dataBaseMessage.setUserName(user);
        dataBaseMessage.setPassword(password);
        masterDataBaseMessage = dataBaseMessage;
        return dataBaseMessage;
    }

    @Bean(name = "clusterDataSource")
    public DataSource clusterDataSource() {
        MultiDataSource multiDataSource = MultiDataSource.getInstance();
        multiDataSource.switchDataSource(masterDatabaseMessage());
        return multiDataSource;
    }

    @Bean(name = "clusterTransactionManager")
    public DataSourceTransactionManager clusterTransactionManager() {
        return new DataSourceTransactionManager(clusterDataSource());
    }

    @Bean(name = "clusterSqlSessionFactory")
    public SqlSessionFactory clusterSqlSessionFactory(@Qualifier("clusterDataSource") DataSource clusterDataSource)
            throws Exception {
        final SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(clusterDataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources(DataSourceConfig.MAPPER_LOCATION));

        return sessionFactory.getObject();
    }
}