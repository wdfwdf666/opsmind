package com.opsmind.knowledge.document.infrastructure;

import com.opsmind.knowledge.document.application.DocumentParser;
import com.opsmind.knowledge.document.application.ParsedDocument;
import com.opsmind.knowledge.document.config.DocumentProperties;
import com.opsmind.knowledge.document.domain.DocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TikaDocumentParser implements DocumentParser {

    private final int maxExtractedCharacters;

    public TikaDocumentParser(DocumentProperties properties) {
        this.maxExtractedCharacters = properties.maxExtractedCharacters();
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename, String declaredMediaType) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        metadata.set(Metadata.CONTENT_TYPE, declaredMediaType);
        BodyContentHandler handler = new BodyContentHandler(maxExtractedCharacters);
        try {
            new AutoDetectParser().parse(inputStream, handler, metadata, new ParseContext());
            String detectedMediaType = metadata.get(Metadata.CONTENT_TYPE);
            return new ParsedDocument(
                    metadata.get(TikaCoreProperties.TITLE),
                    detectedMediaType == null ? declaredMediaType : detectedMediaType,
                    handler.toString());
        } catch (SAXException exception) {
            if (WriteLimitReachedException.isWriteLimitReached(exception)) {
                throw new DocumentException(
                        "DOCUMENT_EXTRACTED_TEXT_TOO_LARGE",
                        "Extracted document text exceeds the configured limit",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
            throw new DocumentException(
                    "DOCUMENT_PARSE_FAILED",
                    "Failed to parse the stored document",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException | TikaException exception) {
            throw new DocumentException(
                    "DOCUMENT_PARSE_FAILED",
                    "Failed to parse the stored document",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
