package message.mybatis.key.generic;

import message.mybatis.key.AbstractMaxValueIncrementer;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQL主键自增长策略
 *
 * @author sunhao(sunhao.java@gmail.com)
 * @version V1.0, 2012-4-11 上午09:15:08
 * @see org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer
 */
public class MySQLMaxValueIncrementer extends AbstractMaxValueIncrementer {
	/** The SQL string for retrieving the new sequence value */
	private static final String VALUE_SQL = "select last_insert_id()";

	/** The next id to serve */
	private long nextId = 0;

	/** The max id to serve */
	private long maxId = 0;
	
	/** The number of keys buffered in a cache */
	private int cacheSize = 1;

    /**cache for each sequence's next id**/
    private Map<String, Long> NEXT_ID_CACHE = new HashMap<String, Long>();

    /**cache for each sequence's max id**/
    private Map<String, Long> MAX_ID_CACHE = new HashMap<String, Long>();

	protected synchronized long getNextKey(String name) throws DataAccessException {
        if(NEXT_ID_CACHE.get(name) != null)
            this.nextId = NEXT_ID_CACHE.get(name);
        if(MAX_ID_CACHE.get(name) != null)
            this.maxId = MAX_ID_CACHE.get(name);

		if (this.maxId == this.nextId) {
			/*
			* Need to use straight JDBC code because we need to make sure that the insert and select
			* are performed on the same connection (otherwise we can't be sure that last_insert_id()
			* returned the correct value)
			*/
			Connection con = DataSourceUtils.getConnection(getDataSource());
			Statement stmt = null;
			try {
				stmt = con.createStatement();
				DataSourceUtils.applyTransactionTimeout(stmt, getDataSource());
                String updateSql = "update "+ name + " set " + name + " = last_insert_id(" + name + " + " + getCacheSize() + ")";
                try {
                    // Increment the sequence column...
                    stmt.executeUpdate(updateSql);
                } catch (SQLException e) {
                    //发生异常,即不存在这张表
                    //1.执行新建这张表的语句
                    StringBuffer createSql = new StringBuffer();
                    createSql.append("create table ").append(name).append(" ( ");
                    createSql.append(name).append(" bigint(12) default null ");
                    createSql.append(" ) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ");
                    stmt.execute(createSql.toString());
                    //2.初始化这张表的数据(将name的值置为0)
                    String initDataSql = "insert into " + name + " values(0)";
                    stmt.executeUpdate(initDataSql);
                    //3.Increment the sequence column...
                    stmt.executeUpdate(updateSql);
                }
                // Retrieve the new max of the sequence column...
				ResultSet rs = stmt.executeQuery(VALUE_SQL);
				try {
					if (!rs.next()) {
						throw new DataAccessResourceFailureException("last_insert_id() failed after executing an update");
					}
					this.maxId = rs.getLong(1);
                    //更新缓存
                    MAX_ID_CACHE.put(name, this.maxId);
				}
				finally {
					JdbcUtils.closeResultSet(rs);
				}
				this.nextId = this.maxId - getCacheSize() + 1;
                //更新缓存
                NEXT_ID_CACHE.put(name, this.nextId);
			} catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not obtain last_insert_id()", ex);
			} finally {
				JdbcUtils.closeStatement(stmt);
				DataSourceUtils.releaseConnection(con, getDataSource());
			}
		}
		else {
			this.nextId++;
            //更新缓存
            NEXT_ID_CACHE.put(name, this.nextId);
		}
        long result = this.nextId;

        //初始化nextId和maxId
        this.nextId = 0;
        this.maxId = 0;
		return result;
	}

	/**
	 * Return the number of buffered keys.
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Set the number of buffered keys.
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

}
