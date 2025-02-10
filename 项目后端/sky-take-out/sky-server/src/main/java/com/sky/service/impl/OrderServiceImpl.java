package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;


    @Value("${sky.shop.lng}")
    private String Shoplng;

    @Value("${sky.shop.lat}")
    private String Shoplat;

    @Value("${sky.tengxun.key}")
    private String key;

    @Value("${sky.tengxun.sk}")
    private String sk;

    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理异常（地址簿为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userid = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userid).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 检查配送距离是否超过5KM
        checkOutOfRange(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        // 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setUserId(userid);
        orderMapper.insert(orders);
        // 向订单明细表插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); // 设置id
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        shoppingCartMapper.deleteAll(shoppingCart);
        return OrderSubmitVO.builder()
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (orders == null) {
            throw new OrderBusinessException("订单号不存在");
        }
        if (!Orders.PAID.equals(orders.getStatus())){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
        JSONObject jsonObject = new JSONObject();
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, ordersPaymentDTO.getOrderNumber());
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId", orders.getId());
        map.put("content","订单号: " + orders.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult pageQuery(int page, int pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> p = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList();
        if(p != null && p.getTotal() > 0) {
            for(Orders orders : p) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(p.getTotal(),list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    @Override
    public void userCancelById(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if (Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> p = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = getOrderVOList(p);
        return new PageResult(p.getTotal(),list);
    }

    @Override
    public OrderStatisticsVO statistics() {
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                        .id(ordersConfirmDTO.getId())
                        .status(Orders.CONFIRMED)
                        .build();
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orderDB = orderMapper.getById(ordersRejectionDTO.getId());
        if(orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if( !Orders.TO_BE_CONFIRMED.equals(orderDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orderDB = orderMapper.getById(ordersCancelDTO.getId());
        if(orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if( !Orders.CONFIRMED.equals(orderDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if( !Orders.CONFIRMED.equals(orderDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if( !Orders.DELIVERY_IN_PROGRESS.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号:"+ordersDB.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for(Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String Dishstr = getOrderDishesStr(orders);
                orderVO.setOrderDishes(Dishstr);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    public String getOrderDishesStr(Orders orders){
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> str = orderDetailList.stream().map(s -> s.getName() + "*" + s.getNumber()).collect(Collectors.toList());
        return String.join(" ", str);
    }

    private void checkOutOfRange(String address) {
        Map<String,String> map1 = new LinkedHashMap<>();
        map1.put("address", address);
        map1.put("key",key);
        StringBuilder sigMD51 = new StringBuilder("/ws/geocoder/v1/?");
        map1 = getMapQuery(map1,sigMD51,sk);
        String userCoordinate = HttpClientUtil.doGet("https://apis.map.qq.com/ws/geocoder/v1/",map1);
        JSONObject jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String UserLngLat = lat + "," + lng;
        String ShopLngLat = Shoplat + "," + Shoplng;
        Map<String,String> map2 = new LinkedHashMap<>();
        map2.put("from",ShopLngLat);
        map2.put("key",key);
        map2.put("to",UserLngLat);
        StringBuilder sigMD52 = new StringBuilder("/ws/direction/v1/ebicycling/?");
        map2 = getMapQuery(map2,sigMD52,sk);
        String DistanceJSON = HttpClientUtil.doGet("https://apis.map.qq.com/ws/direction/v1/ebicycling/",map2);
        jsonObject = JSON.parseObject(DistanceJSON);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("距离解析失败");
        }
        int distance = jsonObject.getJSONObject("result").getJSONArray("routes").getJSONObject(0).getIntValue("distance");
        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    public Map<String,String> getMapQuery(Map<String,String> map,StringBuilder strbuilder,String valueSk) {
        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = map.get(key);
            System.out.println("key:"+key+",value:"+value);
            strbuilder.append(key).append("=").append(value);
            if (iterator.hasNext()) {
                strbuilder.append("&");
            }
        }
        strbuilder.append(valueSk);
        String sig = DigestUtils.md5DigestAsHex(strbuilder.toString().getBytes()).toLowerCase();
        map.put("sig",sig);
        return map;
    }
}
