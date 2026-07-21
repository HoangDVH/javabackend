package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.UserAddressRequest;
import com.hoang.jwtjava.dto.request.UserUpdateRequest;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.entity.UserAddress;
import com.hoang.jwtjava.mapper.UserMapper;
import com.hoang.jwtjava.repository.UserAddressRepository;
import com.hoang.jwtjava.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileAndAddressServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfileSetsFullNameAndPhone() {
        User user = User.builder()
                .id("u1")
                .email("a@example.com")
                .roles(Set.of("USER"))
                .build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return com.hoang.jwtjava.dto.response.UserResponse.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .phone(u.getPhone())
                    .roles(u.getRoles())
                    .build();
        });

        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .build();

        var response = userService.updateUserByEmail("a@example.com", request);
        assertEquals("Nguyễn Văn A", response.getFullName());
        assertEquals("0901234567", response.getPhone());
    }

    @Test
    void createAddressClearsPreviousDefault() {
        UserAddressService addressService = new UserAddressService(userAddressRepository, userRepository);
        User user = User.builder().id("u1").email("a@example.com").build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(userAddressRepository.save(any())).thenAnswer(inv -> {
            UserAddress a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        UserAddressRequest request = new UserAddressRequest();
        request.setLabel("Nhà");
        request.setReceiverName("Nguyễn Văn A");
        request.setPhone("0901234567");
        request.setAddress("123 Nguyễn Huệ");
        request.setIsDefault(true);

        var response = addressService.create("a@example.com", request);

        verify(userAddressRepository).clearDefaultForUser("u1");
        assertTrue(response.isDefault());
        assertEquals("Nguyễn Văn A", response.getReceiverName());
    }

    @Test
    void firstAddressBecomesDefaultAutomatically() {
        UserAddressService addressService = new UserAddressService(userAddressRepository, userRepository);
        User user = User.builder().id("u1").email("a@example.com").build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(userAddressRepository.findByUser_EmailOrderByIsDefaultDescIdAsc("a@example.com"))
                .thenReturn(new ArrayList<>());
        when(userAddressRepository.save(any())).thenAnswer(inv -> {
            UserAddress a = inv.getArgument(0);
            a.setId(3L);
            return a;
        });

        UserAddressRequest request = new UserAddressRequest();
        request.setReceiverName("A");
        request.setPhone("0901");
        request.setAddress("Addr");
        request.setIsDefault(false);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        var response = addressService.create("a@example.com", request);
        verify(userAddressRepository).save(captor.capture());
        assertTrue(captor.getValue().isDefault());
        assertTrue(response.isDefault());
        assertFalse(request.getIsDefault());
    }
}
