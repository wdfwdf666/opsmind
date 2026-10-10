package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.domain.LexicalChunkRepository;
import com.opsmind.knowledge.document.domain.SearchableChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcLexicalChunkRepository implements LexicalChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLexicalChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SearchableChunk> findCandidates(UUID documentId) {
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
