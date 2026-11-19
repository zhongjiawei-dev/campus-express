package com.campus.express.station;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.express.common.Result;
import com.campus.express.station.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationMapper stationMapper;

    @GetMapping
    public Result<List<Station>> list() {
        return Result.ok(stationMapper.selectList(new LambdaQueryWrapper<Station>()
                .eq(Station::getStatus, 1)
                .orderByAsc(Station::getSort)));
    }

    @GetMapping("/{id}")
    public Result<Station> detail(@PathVariable Long id) {
        return Result.ok(stationMapper.selectById(id));
    }
}
