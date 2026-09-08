package com.seochang.church.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BibleServiceTests {

    private final BibleService bibleService = new BibleService(null);

    @Test
    @DisplayName("한국 천주교회 공용 번역본(catholic) URL 생성 검증: 구약은 101~146, 신약은 147~173 매핑")
    void buildBibleUrlForCatholicTranslation() {
        // 구약 1권 (창세기) -> m=1, n=101
        String genesisUrl = bibleService.buildBibleUrl(1, 1, 1, "catholic");
        assertThat(genesisUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=1&n=101&p=1");

        // 구약 46권 (말라키서) -> m=1, n=146
        String malachiUrl = bibleService.buildBibleUrl(1, 46, 3, "catholic");
        assertThat(malachiUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=1&n=146&p=3");

        // 신약 1권 (마태오 복음서) -> m=2, n=147
        String matthewUrl = bibleService.buildBibleUrl(2, 1, 1, "catholic");
        assertThat(matthewUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=2&n=147&p=1");

        // 신약 27권 (요한 묵시록) -> m=2, n=173
        String revelationUrl = bibleService.buildBibleUrl(2, 27, 22, "catholic");
        assertThat(revelationUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=2&n=173&p=22");

        // version이 null 또는 알 수 없는 값일 때도 기본값은 catholic
        String defaultUrl = bibleService.buildBibleUrl(1, 1, 1, null);
        assertThat(defaultUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=1&n=101&p=1");
    }

    @Test
    @DisplayName("공동번역 성서(joint) URL 생성 검증: 구약은 1~46, 신약은 47~73 매핑")
    void buildBibleUrlForJointTranslation() {
        // 구약 1권 -> m=1, n=1
        String genesisJointUrl = bibleService.buildBibleUrl(1, 1, 1, "joint");
        assertThat(genesisJointUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=1&n=1&p=1");

        // 구약 46권 -> m=1, n=46
        String malachiJointUrl = bibleService.buildBibleUrl(1, 46, 3, "joint");
        assertThat(malachiJointUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=1&n=46&p=3");

        // 신약 1권 -> m=2, n=47
        String matthewJointUrl = bibleService.buildBibleUrl(2, 1, 1, "joint");
        assertThat(matthewJointUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=2&n=47&p=1");

        // 신약 27권 -> m=2, n=73
        String revelationJointUrl = bibleService.buildBibleUrl(2, 27, 22, "joint");
        assertThat(revelationJointUrl).isEqualTo("https://maria.catholic.or.kr/bible/read/bible_read.asp?m=2&n=73&p=22");
    }

    @Test
    @DisplayName("GoodNews 성경 본문 HTML 파싱 검증: 소제목, 본문 절, NAB 영문 행 필터링")
    void parseVersesFromHtml() {
        String html = """
                <table>
                    <tbody>
                        <tr>
                            <td class="num_color"></td>
                            <td class="al tt"><span>천지 창조</span></td>
                        </tr>
                        <tr class="trlt">
                            <td colspan="7" class="sub_tt"><span>▶ First Story of Creation</span></td>
                        </tr>
                        <tr>
                            <td class="num_color"><span>1</span></td>
                            <td class="al tt"><span>한처음에 하느님께서 하늘과 땅을 창조하셨다.</span></td>
                        </tr>
                        <tr class="trlt">
                            <td colspan="7" class="sub_tt"><span>▶ In the beginning, when God created the heavens and the earth</span></td>
                        </tr>
                        <tr>
                            <td class="num_color"><span>2</span></td>
                            <td class="al tt"><span>땅은 아직 꼴을 갖추지 못하고 비어 있었는데...</span></td>
                        </tr>
                    </tbody>
                </table>
                """;

        Document doc = Jsoup.parse(html);
        List<Map<String, String>> verses = bibleService.parseVerses(doc);

        assertThat(verses).hasSize(3);

        // 소제목
        assertThat(verses.get(0).get("verse")).isEmpty();
        assertThat(verses.get(0).get("text")).isEqualTo("천지 창조");

        // 1절
        assertThat(verses.get(1).get("verse")).isEqualTo("1");
        assertThat(verses.get(1).get("text")).isEqualTo("한처음에 하느님께서 하늘과 땅을 창조하셨다.");

        // 2절
        assertThat(verses.get(2).get("verse")).isEqualTo("2");
        assertThat(verses.get(2).get("text")).isEqualTo("땅은 아직 꼴을 갖추지 못하고 비어 있었는데...");
    }
}
