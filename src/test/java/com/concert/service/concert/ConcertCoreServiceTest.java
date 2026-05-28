package com.concert.service.concert;

import com.concert.dto.response.ConcertDetailResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.service.concert.impl.ConcertCoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ConcertCoreService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ConcertCoreServiceTest {

    @Mock
    private ConcertService concertService;

    @Mock
    private ConcertArtistService concertArtistService;

    @Mock
    private ArtistService artistService;

    @Mock
    private ShowService showService;

    @Mock
    private VenueService venueService;

    @InjectMocks
    private ConcertCoreServiceImpl concertCoreService;

    private Concert testConcert;
    private Artist testArtist;
    private ConcertArtist testConcertArtist;
    private Show testShow;
    private Venue testVenue;

    @BeforeEach
    void setUp() {
        testConcert = new Concert();
        testConcert.setId(1L);
        testConcert.setName("测试演唱会");
        testConcert.setPoster("poster.jpg");
        testConcert.setDescription("描述信息");
        testConcert.setStatus(ConcertStatus.IN_PROGRESS);

        testArtist = new Artist();
        testArtist.setId(10L);
        testArtist.setName("测试艺人");
        testArtist.setAvatar("artist_avatar.jpg");
        testArtist.setDescription("艺人简介");

        testConcertArtist = new ConcertArtist();
        testConcertArtist.setId(1L);
        testConcertArtist.setConcertId(1L);
        testConcertArtist.setArtistId(10L);

        testShow = new Show();
        testShow.setId(100L);
        testShow.setConcertId(1L);
        testShow.setVenueId(200L);
        testShow.setShowTime(LocalDateTime.now().plusDays(7));
        testShow.setStatus(ShowStatus.ON_SALE);

        testVenue = new Venue();
        testVenue.setId(200L);
        testVenue.setName("测试场馆");
        testVenue.setCity("北京");
        testVenue.setAddress("朝阳区xxx");
    }

    @Nested
    @DisplayName("获取演唱会详情 - 正常流程")
    class GetConcertDetailNormal {

        @Test
        @DisplayName("包含艺人和场次信息的完整详情")
        void testGetConcertDetail_WithArtistsAndShows() {
            // Given
            when(concertService.getById(1L)).thenReturn(testConcert);
            when(concertArtistService.list(any())).thenReturn(Collections.singletonList(testConcertArtist));
            when(artistService.listByIds(Collections.singletonList(10L))).thenReturn(Collections.singletonList(testArtist));
            when(showService.list(any())).thenReturn(Collections.singletonList(testShow));
            when(venueService.listByIds(Collections.singletonList(200L))).thenReturn(Collections.singletonList(testVenue));

            // When
            ConcertDetailResponse response = concertCoreService.getConcertDetail(1L);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("测试演唱会", response.getName());
            assertEquals(1, response.getArtists().size());
            assertEquals("测试艺人", response.getArtists().get(0).getName());
            assertEquals(1, response.getShows().size());
            assertEquals("测试场馆", response.getShows().get(0).getVenueName());
        }

        @Test
        @DisplayName("无艺人关联时返回空列表")
        void testGetConcertDetail_NoArtists() {
            // Given
            when(concertService.getById(1L)).thenReturn(testConcert);
            when(concertArtistService.list(any())).thenReturn(Collections.emptyList());
            when(showService.list(any())).thenReturn(Collections.emptyList());

            // When
            ConcertDetailResponse response = concertCoreService.getConcertDetail(1L);

            // Then
            assertNotNull(response);
            assertTrue(response.getArtists().isEmpty());
            assertTrue(response.getShows().isEmpty());
        }
    }

    @Nested
    @DisplayName("获取演唱会详情 - 异常场景")
    class GetConcertDetailException {

        @Test
        @DisplayName("演唱会不存在抛出NotFoundException")
        void testGetConcertDetail_NotFound_ThrowsException() {
            // Given
            when(concertService.getById(999L)).thenReturn(null);

            // When & Then
            assertThrows(NotFoundException.class, () -> concertCoreService.getConcertDetail(999L));
        }
    }

    @Nested
    @DisplayName("获取演唱会详情 - 边界条件")
    class GetConcertDetailBoundary {

        @Test
        @DisplayName("多个艺人和多个场次")
        void testGetConcertDetail_MultipleArtistsAndShows() {
            // Given
            Artist artist2 = new Artist();
            artist2.setId(11L);
            artist2.setName("艺人2");

            ConcertArtist ca2 = new ConcertArtist();
            ca2.setId(2L);
            ca2.setConcertId(1L);
            ca2.setArtistId(11L);

            Show show2 = new Show();
            show2.setId(101L);
            show2.setConcertId(1L);
            show2.setVenueId(200L);
            show2.setShowTime(LocalDateTime.now().plusDays(8));
            show2.setStatus(ShowStatus.ON_SALE);

            when(concertService.getById(1L)).thenReturn(testConcert);
            when(concertArtistService.list(any())).thenReturn(Arrays.asList(testConcertArtist, ca2));
            when(artistService.listByIds(anyList())).thenReturn(Arrays.asList(testArtist, artist2));
            when(showService.list(any())).thenReturn(Arrays.asList(testShow, show2));
            when(venueService.listByIds(anyList())).thenReturn(Collections.singletonList(testVenue));

            // When
            ConcertDetailResponse response = concertCoreService.getConcertDetail(1L);

            // Then
            assertEquals(2, response.getArtists().size());
            assertEquals(2, response.getShows().size());
        }

        @Test
        @DisplayName("场次关联场馆不存在时不崩溃")
        void testGetConcertDetail_VenueNotFound_NoCrash() {
            // Given
            when(concertService.getById(1L)).thenReturn(testConcert);
            when(concertArtistService.list(any())).thenReturn(Collections.emptyList());
            when(showService.list(any())).thenReturn(Collections.singletonList(testShow));
            when(venueService.listByIds(anyList())).thenReturn(Collections.emptyList());

            // When
            ConcertDetailResponse response = concertCoreService.getConcertDetail(1L);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getShows().size());
            assertNull(response.getShows().get(0).getVenueName());
        }
    }
}
