package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

import com.opsmind.knowledge.document.domain.LexicalChunkRepository;
import com.opsmind.knowledge.document.domain.SearchableChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository // 注解作用：标记数据库访问组件，并启用数据访问异常转换。
public class JdbcLexicalChunkRepository implements LexicalChunkRepository { // 类型作用：JDBC 仓储实现，使用参数化 SQL 读写数据库。

    private final JdbcTemplate jdbcTemplate;

    public JdbcLexicalChunkRepository(JdbcTemplate jdbcTemplate) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public List<SearchableChunk> findCandidates(UUID documentId) { // 方法作用：按条件读取目标数据，不存在时按约定返回空值或异常。
        String documentFilter = documentId == null ? "" : " AND child.document_id = ?";
        String sql = """
                SELECT child.id AS chunk_id, child.parent_id, child.document_id,
                       document.original_filename AS filename,
                       child.heading_path, child.text_content AS child_text,
                       parent.text_content AS parent_text
                FROM document_chunk child
                JOIN document_chunk parent ON parent.id = child.parent_id
                JOIN knowledge_document document ON document.id = child.document_id
                WHERE child.chunk_type = 'CHILD'
                """ + documentFilter + " ORDER BY child.document_id, child.ordinal";
        Object[] arguments = documentId == null ? new Object[0] : new Object[]{documentId};
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new SearchableChunk(
                resultSet.getObject("chunk_id", UUID.class),
                resultSet.getObject("parent_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getString("filename"),
                resultSet.getString("heading_path"),
                resultSet.getString("child_text"),
                resultSet.getString("parent_text")), arguments);
    }
}
