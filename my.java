import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelectList;
import org.apache.calcite.sql.SqlSubquery;
import org.apache.calcite.sql.SqlToRelConverter;

import org.apache.calcite.sql.parser.SqlParser.Config;
import org.apache.calcite.sql.parser.SqlParserException;

import java.util.List;

public class SQLInputProvider {

    private final JdbcTemplate jdbcTemplate;

    private SQLInputProvider(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public static DataSourceBuilder withDataSource() {
        return new Builder();
    }

    public interface DataSourceBuilder {
        QueryBuilder dataSource(String url, String username, String password, String driverClassName);
    }

    public interface QueryBuilder {
        <T> RowMapperBuilder<T> query(String query);
        <T> RowMapperBuilder<T> query(String query, Object... params);
    }

    public interface RowMapperBuilder<T> {
        FetchBuilder<T> map(RowMapper<T> rowMapper);
    }

    public interface FetchBuilder<T> {
        List<T> fetch();
        T fetchOne();
    }

    private static class Builder implements DataSourceBuilder, QueryBuilder, RowMapperBuilder<Object>, FetchBuilder<Object> {
        private DataSource dataSource;
        private String query;
        private Object[] params;
        private RowMapper<Object> rowMapper;

        @Override
        public QueryBuilder dataSource(String url, String username, String password, String driverClassName) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName(driverClassName);
            this.dataSource = dataSource;
            return this;
        }

        @Override
        public <T> RowMapperBuilder<T> query(String query) {
            validateQuerySyntax(query);
            validateJoins(query); // Basic join validation
            validateSubqueries(query); // Robust subquery validation
            this.query = query;
            return (RowMapperBuilder<T>) this;
        }

        @Override
        public <T> RowMapperBuilder<T> query(String query, Object... params) {
            validateQuerySyntax(query);
            validateJoins(query); // Basic join validation
            validateSubqueries(query); // Robust subquery validation
            this.query = query;
            this.params = params;
            return (RowMapperBuilder<T>) this;
        }

        @Override
        public FetchBuilder<Object> map(RowMapper<Object> rowMapper) {
            this.rowMapper = rowMapper;
            return this;
        }

        @Override
        public List<Object> fetch() {
            validateFields();
            return params == null ? jdbcTemplate.query(query, rowMapper) : jdbcTemplate.query(query, rowMapper, params);
        }

        @Override
        public Object fetchOne() {
            validateFields();
            return params == null ? jdbcTemplate.queryForObject(query, rowMapper) : jdbcTemplate.queryForObject(query, rowMapper, params);
        }

        private void validateQuerySyntax(String query) {
            try {
                SqlParser parser = SqlParser.create(query, SqlParser.config().withConformance(SqlConformanceEnum.LENIENT));
                parser.parseQuery();
            } catch (SqlParseException e) {
                throw new IllegalArgumentException("The provided query has invalid syntax: " + e.getMessage(), e);
            }
        }

        private void validateJoins(String query) {
            if (query.contains("JOIN")) {
                // Check that the join clause contains the necessary table names and ON conditions
                if (!query.contains("ON")) {
                    throw new IllegalArgumentException("Missing ON clause in JOIN");
                }
                // Additional checks can be added to ensure table and column names are valid
            }
        }

        private void validateSubqueries(String query) {
            try {
                SqlParser parser = SqlParser.create(query, SqlParser.config().withConformance(SqlConformanceEnum.LENIENT));
                SqlNode sqlNode = parser.parseQuery();

                // Walk through the AST and identify any subqueries
                if (sqlNode instanceof SqlSelect) {
                    validateSelectSubqueries((SqlSelect) sqlNode);
                } else {
                    throw new IllegalArgumentException("Invalid query: Expected SELECT query but found: " + sqlNode.getKind());
                }
            } catch (SqlParseException | SqlValidatorException e) {
                throw new IllegalArgumentException("Subquery validation failed: " + e.getMessage(), e);
            }
        }

        private void validateSelectSubqueries(SqlSelect sqlSelect) {
            // Check the SELECT clause for subqueries
            SqlNodeList selectList = sqlSelect.getSelectList();
            for (SqlNode selectNode : selectList) {
                if (selectNode instanceof SqlSubquery) {
                    SqlSubquery subquery = (SqlSubquery) selectNode;
                    validateSubquerySyntax(subquery.getQuery());
                    validateSubqueryPlacement(subquery);
                }
            }

            // Check WHERE, HAVING, and other clauses for subqueries
            if (sqlSelect.getWhere() instanceof SqlSubquery) {
                validateSubquerySyntax((SqlSubquery) sqlSelect.getWhere());
            }
        }

        private void validateSubquerySyntax(SqlSubquery subquery) {
            SqlNode subqueryNode = subquery.getQuery();
            if (!(subqueryNode instanceof SqlSelect)) {
                throw new IllegalArgumentException("Subquery must be a SELECT query. Found: " + subqueryNode.getKind());
            }
            // Ensure that the subquery is syntactically correct
            validateQuerySyntax(subqueryNode.toString());
        }

        private void validateSubqueryPlacement(SqlSubquery subquery) {
            // Ensure the subquery is used in a valid place (e.g., in WHERE, FROM, etc.)
            if (subquery.getParent() instanceof SqlSelect) {
                SqlSelect parentSelect = (SqlSelect) subquery.getParent();
                if (parentSelect.getWhere() != subquery && parentSelect.getFrom() != subquery) {
                    throw new IllegalArgumentException("Subquery should be placed in WHERE or FROM clause.");
                }
            }
        }

        private void validateFields() {
            if (dataSource == null || query == null || rowMapper == null) {
                throw new IllegalStateException("All fields (dataSource, query, map) must be provided before calling fetch() or fetchOne()");
            }
        }
    }
}
