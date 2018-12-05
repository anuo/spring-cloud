package com.fangjia.sjdbc;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * mybatis最后由StatementHandler执行SQL语句
 * 在执行sql之前由prepare初始化Statement.例如:设置Statement最大抓取的数据,预编译sql语句
 * 接着由parameterize方法设置SQL参数 最后调用query,update方法执行操作
 * 在这里我们只对StatementHandler的prepare方法进行拦截 Intercepts注解代表这是一个拦截器
 * Signature注解代表要拦截的方法,需要指定拦截的类,方法名称,与方法的参数
 *  
 * @author song
 *
 */
public abstract class MybatisPagePlugin implements Interceptor {

	private static Logger log = LoggerFactory
			.getLogger(MybatisPagePlugin.class);

	protected Integer printSQL = 2;

	/**
	 * 默认当前页码
	 */
	private final Integer DEFAULT_PAGE_NO = 1;

	/**
	 * 默认每页显示的记录数
	 */
	private final Integer DEFAULT_PAGE_SIZE = 20;

	/**
	 * 
	 * 1.判断是不是拦截的StatementHandler对象 2.是就继续拦截执行，否则就跳过执行后续拦截器 3.获取语句映射对象
	 * 4.判断是否是查询，只有查询才使用分页 5.从查询参数中获取分页参数 6.判断是否使用分页 7.验证分页参数，设置分页参数
	 * 8.根据源SQL，构建分页SQL 9.覆盖源SQL，设置为分页SQL 10.根据源SQL，构建查询记录总数SQL
	 * 11.获取Connection对象，构建statement对象，执行查询总记录数SQL，获取总记录数，释放资源 12.设置分页信息
	 * 13.继续执行其它拦截器逻辑
	 * 
	 */
	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		/**
		 * 对StatementHandler进行拦截
		 */
		if (invocation.getTarget() instanceof StatementHandler) {

			MetaObject metaObject = getMetaObject(invocation);

			/**
			 * MappedStatement保存了映射器的一个节点(例如:select|delete等),我们配置的sql,resultMap,
			 * parameterType等我们在节点配置所有信息与全局配置信息
			 */
			MappedStatement mappedStatement = (MappedStatement) metaObject
					.getValue("delegate.mappedStatement");

			/**
			 * BoundSql存放了与sql相关的内容,比如sql语句,sql参数,sql参数对应的映射规则(java--jdbc类型,
			 * 对应类型处理器等)
			 */
			BoundSql boundSql = (BoundSql) metaObject
					.getValue("delegate.boundSql");

			/**
			 * 打印SQL语句和参数，测试要看
			 */
			if (log.isInfoEnabled() && printSQL == 1) {
				StringBuilder sb=new StringBuilder();
				Object obj = boundSql.getParameterObject();
				sb.append("sql语句:"+System.lineSeparator());
				sb.append(beautifySql(boundSql.getSql())+System.lineSeparator());
				if (obj != null) {
					sb.append("sql参数:"+System.lineSeparator());
					if (obj instanceof Map) {
						Map m = (Map) obj;
						Collection keys = m.keySet();
						if (keys != null) {
							for (Object key : keys) {
								sb.append(key + ":" + m.get(key) + System.lineSeparator());
							}
						}
					} else if (!(obj instanceof String)
							&& !(obj instanceof Boolean)
							&& !(obj instanceof Long)
							&& !(obj instanceof Float)
							&& !(obj instanceof Double)
							&& !(obj instanceof Integer)
							&& !(obj instanceof Character)
							&& !(obj instanceof Byte)) {
						Class<?> clazz = obj.getClass();
						Field[] fields = clazz.getDeclaredFields();
						for (int i = 0; i < fields.length; i++) {
							fields[i].setAccessible(true);
							if (fields[i].get(obj) == null) {
								continue;
							}
							sb.append(fields[i].getName() + ":"
									+ fields[i].get(obj) + System.lineSeparator());
						}
					} else {
						sb.append(boundSql.getParameterObject().getClass()
								.getName()
								+ ":" + obj + System.lineSeparator());
					}	
				}
				log.info(sb.toString());
				sb=null;
			}

			/**
			 * 是否是查询操作
			 */
			if (!(SqlCommandType.SELECT == mappedStatement.getSqlCommandType())) {
				return invocation.proceed();
			}

			/**
			 * 从查询参数中获取分页参数
			 */
			PageUnsafe pageUnsafe = getPageUnsafe(boundSql.getParameterObject());

			/**
			 * 是否启用分页
			 */
			if (!isPage(pageUnsafe)) {
				return invocation.proceed();
			}

			if (pageUnsafe.getPageNo() == null || pageUnsafe.getPageNo() < 1) {
				pageUnsafe.setPageNo(DEFAULT_PAGE_NO);
			}
			if (pageUnsafe.getPageSize() == null
					|| pageUnsafe.getPageSize() < 1) {
				pageUnsafe.setPageSize(DEFAULT_PAGE_SIZE);
			}

			PageUnsafe tempPageUnsafe = new PageUnsafe();
			tempPageUnsafe.setIsPage(pageUnsafe.getIsPage());
			tempPageUnsafe.setPageNo(pageUnsafe.getPageNo());
			tempPageUnsafe.setPageSize(pageUnsafe.getPageSize());
			tempPageUnsafe.setTotal(pageUnsafe.getTotal());
			tempPageUnsafe.setTotalPage(pageUnsafe.getTotalPage());

			/**
			 * 根据源sql构建分页语句,这里不同的数据库的分页语句不同
			 */

			String srcSql = boundSql.getSql();

			String pageSql = buildPageSql(srcSql, tempPageUnsafe);

			/**
			 * 将构建的分页语句设置回去,覆盖掉源sql语句,这样接下来mybatis执行的就是我们的分页语句
			 */
			metaObject.setValue("delegate.boundSql.sql", pageSql);

			/**
			 * 分页数据查询出来了,但是还缺少一个分页的重要属性,就是总记录数,我们需要自己去查询出来
			 */
			/**
			 * 根据源sql语句构建查询总记录数的sql语句
			 */
			String countSql = buildPageCountSql(srcSql);

			/**
			 * 获取PreparedStatement
			 */
			Connection conn = (Connection) invocation.getArgs()[0];

			PreparedStatement connStmt = conn.prepareStatement(countSql);

			/**
			 * 构建BoundSql对象.我们使用原来的sql参数和sql参数映射规则初始化
			 */
			BoundSql newCountBoundSql = new BoundSql(
					mappedStatement.getConfiguration(), countSql,
					boundSql.getParameterMappings(),
					boundSql.getParameterObject());

			/**
			 * 构建一个StatementHandler对象,执行sql语句
			 */
			ParameterHandler parameterHandler = new DefaultParameterHandler(
					mappedStatement, boundSql.getParameterObject(),
					newCountBoundSql);
			parameterHandler.setParameters(connStmt);
			ResultSet rs = connStmt.executeQuery();
			Integer total = 0;
			if (rs.next()) {
				total = rs.getInt(1);
			}

			/**
			 * 关闭ResultSet和PreparedStatement释放资源,并且置为null,使GC可以回收,避免内存溢出
			 */
			rs.close();
			rs = null;
			connStmt.close();
			connStmt = null;

			/**
			 * 查询完毕后千万不能关闭connetion对象,mybatis接下来将继续使用此connection对象,
			 * 如果关闭mybatis将无法使用,导致mybatis抛出异常
			 */

			setTotal(total, pageUnsafe);

			/**
			 * 我们的分页代码到此就完毕了,接着继续让mybatis执行,查询出分页数据
			 */
			Object rsObject = invocation.proceed();

			return rsObject;
		}
		return invocation.proceed();
	}

	/**
	 * 是否启用分页
	 * 
	 * @return
	 */
	public Boolean isPage(PageUnsafe pageUnsafe) {
		if (pageUnsafe == null) {
			return false;
		}
		if (pageUnsafe.getIsPage() == null
				|| pageUnsafe.getIsPage().booleanValue() == false) {
			return false;
		}
		return true;
	}

	/**
	 * 处理总记录数
	 * 
	 * @param total
	 */
	public void setTotal(Integer total, PageUnsafe pageUnsafe) {
		if (total == 0) {
			pageUnsafe.setTotal(0);
			pageUnsafe.setTotalPage(0);
		} else {
			pageUnsafe.setTotal(total);
			int pn = pageUnsafe.getTotal() % pageUnsafe.getPageSize();
			if (pn == 0) {
				pageUnsafe.setTotalPage(pageUnsafe.getTotal()
						/ pageUnsafe.getPageSize());
			} else {
				pageUnsafe.setTotalPage(pageUnsafe.getTotal()
						/ pageUnsafe.getPageSize() + 1);
			}
		}
	}

	/**
	 * 只代理StatementHandler,避免包装其他对象,浪费性能和内存
	 * 
	 * @param target
	 * @return
	 */
	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler) {
			return Plugin.wrap(target, this);
		} else {
			return target;
		}
	}

	/**
	 * 设置属性
	 */
	@Override
	public void setProperties(Properties properties) {

	}

	/**
	 * 获取分页参数
	 * 
	 * @param parameterObject
	 * @return
	 */
	private PageUnsafe getPageUnsafe(Object parameterObject) {
		if (parameterObject instanceof Map) {
			Map map = (Map) parameterObject;
			Set keys = map.keySet();
			Iterator it = keys.iterator();
			while (it.hasNext()) {
				Object v = map.get(it.next());
				if (v instanceof PageUnsafe) {
					return (PageUnsafe) v;
				}
			}
		} else if (parameterObject instanceof PageUnsafe) {
			return (PageUnsafe) parameterObject;
		}
		return null;
	}

	/**
	 * 获取代理的真实对象
	 * 
	 * @param invocation
	 * @return
	 */
	private MetaObject getMetaObject(Invocation invocation) {
		MetaObject metaObject = SystemMetaObject.forObject(invocation
				.getTarget());

		while (metaObject.hasGetter("h")) {
			Object obj = metaObject.getValue("h");
			metaObject = SystemMetaObject.forObject(obj);// 将对象包装成MetaObject对象,MetaObject对象通过反射技术可以操作真实对象的所有属性
		}
		while (metaObject.hasGetter("target")) {
			Object obj = metaObject.getValue("target");
			metaObject = SystemMetaObject.forObject(obj);
		}

		/**
		 * 经过如上代码之后,我们就获得了真实的对象,它被包装在metaObject里面 我们通过metaObject提供的方法获取真实的对象的属性值
		 */

		return metaObject;
	}

	public Integer getPrintSQL() {
		return printSQL;
	}

	public void setPrintSQL(Integer printSQL) {
		this.printSQL = printSQL;
	}

	/**
	 * 生成分页语句
	 * 
	 * @param sql
	 * @param offset
	 * @param limit
	 * @return
	 */
	protected abstract String buildPageSql(String sql, PageUnsafe pageUnsafe);

	/**
	 * 生成分页总记录数语句
	 * 
	 * @param sql
	 * @return
	 */
	protected abstract String buildPageCountSql(String sql);

	/**
	 * 分页参数类
	 * 
	 * @author song
	 *
	 */
	public static class PageUnsafe implements Serializable {

		private static final long serialVersionUID = 2451970007428479040L;

		private Integer pageNo;

		private Integer pageSize;

		private Integer total;

		private Integer totalPage;

		private Boolean isPage;

		public Integer getPageNo() {
			return pageNo;
		}

		public void setPageNo(Integer pageNo) {
			this.pageNo = pageNo;
		}

		public Integer getPageSize() {
			return pageSize;
		}

		public void setPageSize(Integer pageSize) {
			this.pageSize = pageSize;
		}

		public Integer getTotal() {
			return total;
		}

		public void setTotal(Integer total) {
			this.total = total;
		}

		public Integer getTotalPage() {
			return totalPage;
		}

		public void setTotalPage(Integer totalPage) {
			this.totalPage = totalPage;
		}

		public Boolean getIsPage() {
			return isPage;
		}

		public void setIsPage(Boolean isPage) {
			this.isPage = isPage;
		}

	}

	/**
	 * 
	* @Title: beautifySql
	* @Description: 美化输出的SQL语句
	* @param @param sql
	* @param @return
	* @return String
	* @throws
	 */
	protected String beautifySql(String sql) {
		sql = sql.replaceAll("[\\s\n ]+", " ");
		return sql;
	}

}
