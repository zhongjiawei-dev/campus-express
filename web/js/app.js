new Vue({
  el: '#app',

  data() {
    return {
      token: localStorage.getItem('ce_token') || '',
      role: localStorage.getItem('ce_role') || 'USER',
      nickname: localStorage.getItem('ce_nickname') || '校园同学',
      view: 'home',
      lastView: 'home',
      loginLoading: false,
      loginError: '',

      types: [
        { value: 1, name: '普通件', desc: '文件、衣物、日用品', extra: 0 },
        { value: 2, name: '大件', desc: '行李箱、被褥、整箱水', extra: 5 },
        { value: 3, name: '生鲜冷链', desc: '水果、乳制品、冷冻食品', extra: 3 }
      ],
      stations: [],
      addresses: [],
      step: 1,
      quote: null,
      quoteLoading: false,
      submitLoading: false,
      form: {
        stationId: null,
        orderType: 1,
        pickupCode: '',
        weight: 2000,
        expectStart: null,
        expectEnd: null,
        addressId: null,
        remark: '',
        insured: false,
        declaredValue: null
      },

      orderTabs: [
        { key: 'all', name: '全部' },
        { key: 'active', name: '进行中' },
        { key: 'done', name: '已完成' }
      ],
      orderTab: 'all',
      orders: [],
      ordersPage: 1,
      ordersHasMore: false,
      ordersLoading: false,

      hallSorts: [
        { key: 'default', name: '最新' },
        { key: 'price_desc', name: '价格高' },
        { key: 'price_asc', name: '价格低' },
        { key: 'distance_asc', name: '距离近' }
      ],
      hallSort: 'default',
      hallOrders: [],
      hallLoading: false,

      detailNo: '',
      detail: null,
      logs: [],
      pickupInput: '',
      photoUrl: 'https://example.com/proof.jpg',
      myReview: null,
      review: { score: 5, tags: [], content: '', anonymous: false },

      wallet: null,
      messages: [],
      unread: 0,
      courier: null,
      todayIncome: 0,

      statusText: {
        10: '待支付', 20: '待接单', 30: '已接单', 40: '已取件',
        50: '已送达', 60: '已完成', 70: '已取消', 80: '退款中',
        90: '已退款', 100: '售后处理中'
      }
    };
  },

  computed: {
    pageTitle() {
      if (this.view === 'detail') return '订单详情';
      return {
        home: '校园快递代取',
        orders: this.role === 'COURIER' ? '我的配送单' : '我的订单',
        hall: '接单大厅',
        mine: '个人中心'
      }[this.view] || '校园快递代取';
    }
  },

  created() {
    if (this.token) {
      const orderNo = new URLSearchParams(location.search).get('order');
      if (orderNo) {
        this.detailNo = orderNo;
        this.view = 'detail';
        this.loadBase().then(() => this.loadDetail());
      } else {
        this.loadBase();
      }
    }
  },

  methods: {
    fmt(value) {
      return fmtTime(value);
    },

    async loginAs(code) {
      this.loginLoading = true;
      this.loginError = '';
      try {
        const data = await api.post('/auth/login', {
          code: code,
          nickname: code === 'courier' ? '周学长' : '林同学'
        });
        this.token = data.token;
        this.role = data.role;
        this.nickname = data.nickname;
        localStorage.setItem('ce_token', data.token);
        localStorage.setItem('ce_role', data.role);
        localStorage.setItem('ce_nickname', data.nickname);
        this.$message.success('已进入' + (data.role === 'COURIER' ? '代取员端' : '学生端'));
        this.view = 'home';
        this.step = 1;
        await this.loadBase();
      } catch (e) {
        this.loginError = e.message || '登录失败，请确认后端已启动';
      } finally {
        this.loginLoading = false;
      }
    },

    logout() {
      localStorage.removeItem('ce_token');
      localStorage.removeItem('ce_role');
      localStorage.removeItem('ce_nickname');
      this.token = '';
      this.role = 'USER';
      this.nickname = '校园同学';
      this.view = 'home';
      this.orders = [];
      this.detail = null;
    },

    go(view) {
      this.lastView = this.view;
      this.view = view;
      if (view === 'home') {
        this.loadBase();
      }
      if (view === 'orders') {
        this.loadOrders(true);
      }
      if (view === 'hall') {
        this.loadHall();
      }
      if (view === 'mine') {
        this.loadMine();
      }
    },

    async loadBase() {
      try {
        const results = await Promise.all([api.get('/stations'), api.get('/addresses')]);
        this.stations = results[0];
        this.addresses = results[1];
        if (!this.form.stationId && this.stations.length) {
          this.form.stationId = this.stations[0].id;
        }
        if (!this.form.addressId && this.addresses.length) {
          this.form.addressId = this.addresses[0].id;
        }
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    nextStep() {
      if (this.step === 1) {
        if (!this.form.stationId) {
          this.$message.warning('请选择快递站点');
          return;
        }
        if (!/^\d{4,8}$/.test(this.form.pickupCode)) {
          this.$message.warning('请填写 4-8 位数字取件码');
          return;
        }
        this.step = 2;
        return;
      }
      if (this.step === 2) {
        if (!this.form.addressId) {
          this.$message.warning('请选择收货地址');
          return;
        }
        if (!this.form.expectStart || !this.form.expectEnd) {
          this.$message.warning('请选择预约开始与结束时间');
          return;
        }
        if (this.form.expectEnd <= this.form.expectStart) {
          this.$message.warning('结束时间需晚于开始时间');
          return;
        }
        this.loadQuote();
      }
    },

    prevStep() {
      this.step = Math.max(1, this.step - 1);
    },

    async loadQuote() {
      this.quoteLoading = true;
      try {
        this.quote = await api.post('/price/quote', {
          stationId: this.form.stationId,
          orderType: this.form.orderType,
          weightGram: this.form.weight,
          addressId: this.form.addressId,
          expectTime: toIso(this.form.expectStart),
          insured: this.form.insured,
          declaredValue: this.form.insured ? this.form.declaredValue : null
        });
        this.step = 3;
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.quoteLoading = false;
      }
    },

    async submitOrder() {
      this.submitLoading = true;
      try {
        const created = await api.post('/orders', {
          stationId: this.form.stationId,
          orderType: this.form.orderType,
          weightGram: this.form.weight,
          pickupCode: this.form.pickupCode,
          addressId: this.form.addressId,
          expectStart: toIso(this.form.expectStart),
          expectEnd: toIso(this.form.expectEnd),
          remark: this.form.remark,
          insured: this.form.insured,
          declaredValue: this.form.insured ? this.form.declaredValue : null,
          discount: 0,
          payType: 1,
          urgent: false
        });
        await api.post('/orders/' + created.orderNo + '/pay');
        this.$message.success('下单并支付成功');
        this.openDetail(created.orderNo);
        this.step = 1;
        this.quote = null;
        this.form.pickupCode = '';
        this.form.remark = '';
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.submitLoading = false;
      }
    },

    switchOrderTab(key) {
      if (this.orderTab === key) return;
      this.orderTab = key;
      this.loadOrders(true);
    },

    async loadOrders(reset) {
      if (this.ordersLoading) return;
      this.ordersLoading = true;
      if (reset) {
        this.ordersPage = 1;
        this.orders = [];
      }
      try {
        const base = this.role === 'COURIER' ? '/courier/orders' : '/orders';
        const data = await api.get(base + '?group=' + this.orderTab +
          '&page=' + this.ordersPage + '&size=10');
        this.orders = reset ? data.records : this.orders.concat(data.records);
        this.ordersHasMore = this.ordersPage < data.pages;
        if (this.ordersHasMore) {
          this.ordersPage += 1;
        }
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.ordersLoading = false;
      }
    },

    switchHallSort(key) {
      this.hallSort = key;
      this.loadHall();
    },

    async loadHall() {
      if (this.role !== 'COURIER') return;
      this.hallLoading = true;
      try {
        const data = await api.get('/orders/hall?sort=' + this.hallSort + '&page=1&size=20');
        this.hallOrders = data.records;
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.hallLoading = false;
      }
    },

    grab(orderNo) {
      this.$confirm('接单后请在预约时段内完成取件，超时可能影响信用分。', '确认接单', {
        confirmButtonText: '确认接单',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await api.post('/orders/' + orderNo + '/grab');
          this.$message.success('接单成功');
          this.openDetail(orderNo);
        } catch (e) {
          this.$message.error(e.message);
          this.loadHall();
        }
      }).catch(() => {});
    },

    openDetail(orderNo) {
      this.lastView = this.view === 'detail' ? 'orders' : this.view;
      this.detailNo = orderNo;
      this.view = 'detail';
      this.loadDetail();
    },

    async loadDetail() {
      this.detail = null;
      try {
        const detail = await api.get('/orders/' + this.detailNo);
        this.detail = detail;
        this.logs = detail.logs || [];
        this.myReview = null;
        this.review = { score: 5, tags: [], content: '', anonymous: false };
        if (detail.order && detail.order.status === 60) {
          try {
            this.myReview = await api.get('/evaluations/' + encodeURIComponent(detail.order.orderNo));
          } catch (e) {
            this.myReview = null;
          }
        }
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    async pay() {
      try {
        await api.post('/orders/' + this.detailNo + '/pay');
        this.$message.success('支付成功');
        this.loadDetail();
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    cancelOrder() {
      this.$confirm('接单后取消将扣除 20% 违约金，确定取消该订单吗？', '取消订单', {
        confirmButtonText: '确定取消',
        cancelButtonText: '再想想',
        type: 'warning'
      }).then(async () => {
        try {
          await api.post('/orders/' + this.detailNo + '/cancel', { reason: '用户主动取消' });
          this.$message.success('订单已取消');
          this.loadDetail();
        } catch (e) {
          this.$message.error(e.message);
        }
      }).catch(() => {});
    },

    confirmReceive() {
      this.$confirm('确认已收到包裹？确认后费用将结算给代取员。', '确认收货', {
        confirmButtonText: '确认收货',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          await api.post('/orders/' + this.detailNo + '/confirm');
          this.$message.success('已确认收货');
          this.loadDetail();
        } catch (e) {
          this.$message.error(e.message);
        }
      }).catch(() => {});
    },

    async submitReview() {
      if (!this.review.score) {
        this.$message.warning('请先选择评分');
        return;
      }
      try {
        await api.post('/evaluations', {
          orderNo: this.detail.order.orderNo,
          score: this.review.score,
          tags: this.review.tags.join(','),
          content: this.review.content,
          anonymous: this.review.anonymous
        });
        this.$message.success('评价已提交');
        this.loadDetail();
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    repeatOrder() {
      const order = this.detail.order;
      this.form = Object.assign({}, this.form, {
        stationId: order.stationId || this.form.stationId,
        orderType: order.orderType,
        weight: order.weightGram,
        addressId: order.addressId || this.form.addressId,
        remark: order.remark || '',
        insured: order.insureFlag === 1,
        declaredValue: order.declaredValue || null,
        pickupCode: '',
        expectStart: null,
        expectEnd: null
      });
      this.quote = null;
      this.step = 1;
      this.view = 'home';
      this.lastView = 'home';
      this.$message.success('已带入上一单信息，请补取件码和预约时间');
    },

    async pick() {
      if (!/^\d{4,8}$/.test(this.pickupInput)) {
        this.$message.warning('请输入正确的取件码');
        return;
      }
      try {
        await api.post('/orders/' + this.detailNo + '/pick', {
          pickupCode: this.pickupInput,
          photoUrl: this.photoUrl
        });
        this.$message.success('取件成功');
        this.loadDetail();
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    async deliver() {
      try {
        await api.post('/orders/' + this.detailNo + '/deliver', { photoUrl: this.photoUrl });
        this.$message.success('已确认送达');
        this.loadDetail();
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    appeal() {
      this.$prompt('请描述问题，如丢件、破损、超时', '发起售后', {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        inputType: 'textarea'
      }).then(async ({ value }) => {
        if (!value) {
          this.$message.warning('请填写问题描述');
          return;
        }
        try {
          await api.post('/orders/' + this.detailNo + '/appeal', {
            type: 1,
            description: value,
            demand: '希望平台介入处理'
          });
          this.$message.success('售后已提交');
          this.loadDetail();
        } catch (e) {
          this.$message.error(e.message);
        }
      }).catch(() => {});
    },

    async loadMine() {
      try {
        const results = await Promise.all([
          api.get('/wallet'),
          api.get('/messages/unread-count'),
          api.get('/messages?page=1&size=5'),
          api.get('/addresses')
        ]);
        this.wallet = results[0];
        this.unread = results[1].count;
        this.messages = results[2];
        this.addresses = results[3];
        if (this.role === 'COURIER') {
          const profile = await api.get('/courier/profile');
          this.courier = profile.courier;
          this.todayIncome = profile.todayIncome;
        }
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    async applyCourier() {
      try {
        await api.post('/courier/apply', {
          realName: '林同学',
          studentNo: '202303704',
          idCard: '330203200401011234',
          studentCardUrl: 'https://example.com/student-card.jpg',
          serviceStart: '08:00',
          serviceEnd: '21:00',
          maxWeightGram: 8000,
          stationIds: '1,2'
        });
        this.$message.success('资料已提交，等待平台审核');
        this.loadMine();
      } catch (e) {
        this.$message.error(e.message);
      }
    },

    async addDemoAddress() {
      try {
        await api.post('/addresses', {
          contactName: '林同学',
          phone: '13800138000',
          campus: '东校区',
          building: '8 号楼',
          room: '302',
          detail: '8 号楼 302，门口有快递架',
          isDefault: true
        });
        this.$message.success('地址已添加');
        await this.loadBase();
        this.loadMine();
      } catch (e) {
        this.$message.error(e.message);
      }
    }
  }
});
