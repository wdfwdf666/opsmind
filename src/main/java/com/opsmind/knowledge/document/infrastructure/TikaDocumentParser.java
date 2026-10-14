package com.opsmind.knowledge.document.infrastructure; // 包作用：基础设施层，实现数据库、文件系统和外部模型调用。

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

@Component // 注解作用：把普通组件交给 Spring 容器管理。
public class TikaDocumentParser implements DocumentParser { // 类型作用：封装 TikaDocumentParser 对应的单一职责，供当前模块协作使用。

    private final int maxExtractedCharacters;

    public TikaDocumentParser(DocumentProperties properties) { // 方法作用：构造对象并保存依赖；这些依赖通常由 Spring 构造器注入。
        this.maxExtractedCharacters = properties.maxExtractedCharacters();
    }

    @Override // 注解作用：表示该方法实现或覆盖了父接口/父类中的方法。
    public ParsedDocument parse(InputStream inputStream, String filename, String declaredMediaType) { // 方法作用：把外部或数据库数据转换成项目内部对象。
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
