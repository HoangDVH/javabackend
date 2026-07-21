package com.hoang.jwtjava.service;

import com.hoang.jwtjava.dto.request.UserAddressRequest;
import com.hoang.jwtjava.dto.response.UserAddressResponse;
import com.hoang.jwtjava.entity.User;
import com.hoang.jwtjava.entity.UserAddress;
import com.hoang.jwtjava.exception.AppException;
import com.hoang.jwtjava.exception.ErrorCode;
import com.hoang.jwtjava.repository.UserAddressRepository;
import com.hoang.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserAddressResponse> listMyAddresses(String userEmail) {
        return userAddressRepository.findByUser_EmailOrderByIsDefaultDescIdAsc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse create(String userEmail, UserAddressRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault())
                || userAddressRepository.findByUser_EmailOrderByIsDefaultDescIdAsc(userEmail).isEmpty();

        if (makeDefault)
            userAddressRepository.clearDefaultForUser(user.getId());

        UserAddress address = UserAddress.builder()
                .user(user)
                .label(trimToNull(request.getLabel()))
                .receiverName(request.getReceiverName().trim())
                .phone(request.getPhone().trim())
                .address(request.getAddress().trim())
                .isDefault(makeDefault)
                .build();

        return toResponse(userAddressRepository.save(address));
    }

    @Transactional
    public UserAddressResponse update(String userEmail, Long addressId, UserAddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUser_Email(addressId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        address.setLabel(trimToNull(request.getLabel()));
        address.setReceiverName(request.getReceiverName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddress(request.getAddress().trim());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            userAddressRepository.clearDefaultForUser(address.getUser().getId());
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            address.setDefault(false);
        }

        return toResponse(userAddressRepository.save(address));
    }

    @Transactional
    public void delete(String userEmail, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUser_Email(addressId, userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        boolean wasDefault = address.isDefault();
        String userId = address.getUser().getId();
        userAddressRepository.delete(address);

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findByUser_EmailOrderByIsDefaultDescIdAsc(userEmail);
            if (!remaining.isEmpty()) {
                UserAddress next = remaining.get(0);
                next.setDefault(true);
                userAddressRepository.save(next);
            } else {
                // no-op; clearDefault already irrelevant after delete
                userAddressRepository.clearDefaultForUser(userId);
            }
        }
    }

    private UserAddressResponse toResponse(UserAddress address) {
        return UserAddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .receiverName(address.getReceiverName())
                .phone(address.getPhone())
                .address(address.getAddress())
                .isDefault(address.isDefault())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
