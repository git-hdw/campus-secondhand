package com.campus.secondhand.service;
import com.campus.secondhand.entity.*;
import com.campus.secondhand.repository.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FavoriteServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final FavoriteRepository favorites = mock(FavoriteRepository.class);
    private final FavoriteService service = new FavoriteService(products, favorites);
    private User user() { User user = new User(); user.setId(2L); return user; }
    @Test void rejectsMissingProductWithoutInsert() {
        assertEquals(404, service.create(user(), 9L).getCode());
        verifyNoInteractions(favorites);
    }
    @Test void duplicateReturnsExistingWithoutAnotherInsert() {
        Product product = new Product(); product.setId(9L); product.setStatus(0);
        when(products.lockById(9L)).thenReturn(product);
        Favorite existing = new Favorite(); existing.setId(1L);
        when(favorites.selectOne(any())).thenReturn(existing);
        assertSame(existing, service.create(user(), 9L).getData());
        verify(favorites, never()).insert(any());
    }
    @Test void rejectsOffShelfProduct() {
        Product product = new Product(); product.setStatus(2);
        when(products.lockById(9L)).thenReturn(product);
        assertEquals(404, service.create(user(), 9L).getCode());
        verifyNoInteractions(favorites);
    }
}
