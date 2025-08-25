package org.example;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConversorEpub {

    public static void main(String[] args) {
        try {
            // 1. Inicializa o objeto EPUB
            Book livro = new Book();
            Metadata metadata = livro.getMetadata();
            metadata.addTitle("Diário Oficial - Edição de 08 de Agosto de 2025");
            metadata.addAuthor(new Author("Gerador", "Automático"));

            // 2. Adiciona a Capa Principal do Jornal (1.png)
            File arquivoCapaPrincipal = new File("src/main/resources/Capas/1.png");
            if (arquivoCapaPrincipal.exists()) {
                Resource coverResource = new Resource(new FileInputStream(arquivoCapaPrincipal), "cover.png");
                livro.setCoverImage(coverResource);
                System.out.println("Capa principal '1.png' adicionada aos metadados.");

                // Adiciona a página da capa principal como primeira seção
                String coverPageContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                        "<head><title>Capa</title><style type=\"text/css\"> body { margin: 0; padding: 0; text-align: center; } img { max-width: 100%; height: 100vh; object-fit: contain; } </style></head>" +
                        "<body>" +
                        "<img src=\"" + coverResource.getHref() + "\" alt=\"Capa do Livro\"/>" +
                        "</body>" +
                        "</html>";
                Resource coverPageResource = new Resource(coverPageContent.getBytes(StandardCharsets.UTF_8), "cover.xhtml");
                livro.addSection("Capa Principal", coverPageResource);
                System.out.println("Página de capa principal (cover.xhtml) adicionada.");
            } else {
                System.out.println("Aviso: Arquivo da capa principal '1.png' não encontrado.");
            }

            // 3. Carrega e limpa o HTML
            File htmlInput = new File("src/main/resources/diario4.html");
            Document doc = Jsoup.parse(htmlInput, "UTF-8", "");

            // Limpeza do HTML
            Safelist safelist = Safelist.relaxed()
                    .addTags("figure", "span", "table", "tbody", "tr", "td")
                    .addAttributes(":all", "style", "class")
                    .addAttributes("img", "src")
                    .addProtocols("img", "src", "data");
            doc = new Cleaner(safelist).clean(doc);
            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).charset(StandardCharsets.UTF_8);

            // 4. Extrai o CSS (será usado em todas as páginas de conteúdo)
            Resource cssResource = null;
            Element styleTag = doc.selectFirst("style");
            if (styleTag != null) {
                cssResource = new Resource(styleTag.html().getBytes(StandardCharsets.UTF_8), "estilos.css");
                livro.addResource(cssResource);
                System.out.println("CSS interno extraído com sucesso.");
            }

            // ===== INÍCIO DA NOVA LÓGICA DE PROCESSAMENTO POR SEÇÕES =====

            // 5. Adiciona IDs únicos para todos os cabeçalhos para o Sumário
            Elements allHeadings = doc.select("h1, h2");
            int headingIndex = 0;
            for (Element heading : allHeadings) {
                String id = normalizarNome(heading.text()) + "-" + (headingIndex++);
                heading.attr("id", id);
            }

            // 6. Divide o HTML em seções baseadas nos H1s e processa cada uma
            Elements sectionsH1 = doc.select("h1");
            TableOfContents toc = livro.getTableOfContents();
            int sectionCounter = 1;

            System.out.println("Iniciando processamento de " + sectionsH1.size() + " seções...");

            for (Element h1 : sectionsH1) {
                System.out.println("\n--- Processando Seção " + sectionCounter + ": " + h1.text() + " ---");

                // 6.1. Adiciona a CAPA DA SEÇÃO (2.png, 3.png, etc.)
                String capaSecaoPath = "src/main/resources/Capas/" + (sectionCounter + 1) + ".png";
                File arquivoCapaSecao = new File(capaSecaoPath);
                if (arquivoCapaSecao.exists()) {
                    String epubCapaHref = "capa_secao_" + sectionCounter + ".png";
                    Resource capaSecaoResource = new Resource(new FileInputStream(arquivoCapaSecao), epubCapaHref);
                    livro.addResource(capaSecaoResource);

                    String capaSecaoPageContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
                            "<head><title>Capa da Seção</title><style type=\"text/css\"> body { margin: 0; padding: 0; text-align: center; } img { max-width: 100%; height: 100vh; object-fit: contain; } </style></head>" +
                            "<body><img src=\"" + epubCapaHref + "\" alt=\"Capa da Seção\"/></body></html>";

                    Resource capaSecaoPageResource = new Resource(capaSecaoPageContent.getBytes(StandardCharsets.UTF_8), "pagina_capa_secao_" + sectionCounter + ".xhtml");
                    livro.addSection("Capa da Seção " + sectionCounter, capaSecaoPageResource);
                    System.out.println("Capa '" + (sectionCounter + 1) + ".png' adicionada para esta seção.");
                } else {
                    System.out.println("Aviso: Capa da seção '" + (sectionCounter + 1) + ".png' não encontrada.");
                }

                // 6.2. Extrai o CONTEÚDO DA SEÇÃO (do H1 atual até o próximo H1)
                StringBuilder sectionHtml = new StringBuilder();
                sectionHtml.append(h1.outerHtml());
                Element nextElement = h1.nextElementSibling();
                while (nextElement != null && !nextElement.tagName().equalsIgnoreCase("h1")) {
                    sectionHtml.append(nextElement.outerHtml());
                    nextElement = nextElement.nextElementSibling();
                }

                // 6.3. Cria a página XHTML para o CONTEÚDO DA SEÇÃO
                StringBuilder xhtmlContent = new StringBuilder();
                xhtmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                        .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n")
                        .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>")
                        .append("<title>").append(h1.text()).append("</title>");
                if (cssResource != null) {
                    xhtmlContent.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(cssResource.getHref()).append("\" />");
                }
                xhtmlContent.append("</head>\n<body>\n").append(sectionHtml.toString()).append("</body>\n</html>");

                String secaoHref = "secao_" + sectionCounter + ".xhtml";
                Resource secaoResource = new Resource(xhtmlContent.toString().getBytes(StandardCharsets.UTF_8), secaoHref);
                livro.addSection(h1.text(), secaoResource);
                System.out.println("Conteúdo da seção adicionado como '" + secaoHref + "'.");

                // 6.4. Constrói o SUMÁRIO para esta seção
                TOCReference h1Ref = toc.addTOCReference(new TOCReference(h1.text(), secaoResource, h1.id()));
                System.out.println("TOC: Adicionado H1 -> " + h1.text());

                // Encontra os H2s dentro do HTML desta seção para criar a hierarquia
                Document sectionDoc = Jsoup.parseBodyFragment(sectionHtml.toString());
                for (Element h2 : sectionDoc.select("h2")) {
                    h1Ref.getChildren().add(new TOCReference(h2.text(), secaoResource, h2.attr("id")));
                    System.out.println("TOC: Adicionado H2 -> " + h2.text());
                }

                sectionCounter++;
            }

            // ===== FIM DA NOVA LÓGICA =====

            // 7. Escreve o arquivo EPUB
            EpubWriter epubWriter = new EpubWriter();
            epubWriter.write(livro, new FileOutputStream("jornal_oficial_com_secoes.epub"));
            System.out.println("\nEPUB gerado com sucesso: jornal_oficial_com_secoes.epub");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String normalizarNome(String texto) {
        return texto.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}