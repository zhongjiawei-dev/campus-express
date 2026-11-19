package com.campus.express.address;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.express.common.AesUtil;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.MaskUtil;
import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import com.campus.express.user.Address;
import com.campus.express.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressMapper addressMapper;
    private final AesUtil aesUtil;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long userId = UserContext.userId();
        List<Address> addresses = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt));
        return Result.ok(addresses.stream().map(this::view).collect(Collectors.toList()));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody AddressRequest request) {
        Long userId = UserContext.userId();
        Address address = new Address();
        address.setUserId(userId);
        fill(address, request);
        addressMapper.insert(address);
        if (Boolean.TRUE.equals(request.isDefault())) {
            setDefault(userId, address.getId());
        }
        return Result.ok(view(address));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody AddressRequest request) {
        Long userId = UserContext.userId();
        Address address = requireOwned(id, userId);
        fill(address, request);
        addressMapper.updateById(address);
        if (Boolean.TRUE.equals(request.isDefault())) {
            setDefault(userId, id);
        }
        return Result.ok(view(address));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Address address = requireOwned(id, UserContext.userId());
        addressMapper.deleteById(address.getId());
        return Result.ok();
    }

    @PostMapping("/{id}/default")
    public Result<Void> makeDefault(@PathVariable Long id) {
        Long userId = UserContext.userId();
        requireOwned(id, userId);
        setDefault(userId, id);
        return Result.ok();
    }

    private Address requireOwned(Long id, Long userId) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "地址不存在");
        }
        return address;
    }

    private void setDefault(Long userId, Long id) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0));
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .set(Address::getIsDefault, 1));
    }

    private void fill(Address address, AddressRequest request) {
        if (request.building() == null || request.room() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请填写楼栋与房间号");
        }
        address.setContactName(request.contactName());
        address.setPhoneMask(MaskUtil.phone(request.phone()));
        address.setCampus(request.campus());
        address.setBuilding(request.building());
        address.setRoomMask(request.room());
        address.setDetail(aesUtil.encrypt(request.detail()));
        address.setLat(request.lat() == null ? new BigDecimal("30.2741") : request.lat());
        address.setLng(request.lng() == null ? new BigDecimal("120.1551") : request.lng());
        address.setStatus(0);
    }

    /**
     * 对外输出时解密详细地址；对历史明文数据做兼容降级，避免旧数据无法展示。
     */
    private Map<String, Object> view(Address address) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", address.getId());
        view.put("contactName", address.getContactName());
        view.put("phoneMask", address.getPhoneMask());
        view.put("campus", address.getCampus());
        view.put("building", address.getBuilding());
        view.put("roomMask", address.getRoomMask());
        view.put("detail", safeDecrypt(address.getDetail()));
        view.put("lat", address.getLat());
        view.put("lng", address.getLng());
        view.put("isDefault", address.getIsDefault());
        view.put("createdAt", address.getCreatedAt());
        return view;
    }

    private String safeDecrypt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return aesUtil.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }

    public record AddressRequest(
            String contactName,
            String phone,
            String campus,
            String building,
            String room,
            String detail,
            BigDecimal lat,
            BigDecimal lng,
            Boolean isDefault
    ) {
    }
}
