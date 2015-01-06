package com.aote.rs;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.collection.PersistentSet;
import org.hibernate.proxy.map.MapProxy;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.aote.rs.SellSer.HibernateSQLCall;

@Path("hand")
@Component
public class BusinessService {

	static Logger log = Logger.getLogger(BusinessService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;
	
	//�򵥲��񱨱�
	@GET
	@Path("statement/{sgnetwork}")
	public String dayStatement(@PathParam("sgnetwork") String sgnetwork){
		try{
		Date date = new Date();
		String result="";
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		String sql ="select " +
				"SUM(f_pregas) f_pregas,SUM(f_preamount) f_preamount," +
				"SUM(f_zhye) f_zhye,SUM(f_zhinajin) f_zhinajin," +
				"SUM(f_grossproceeds) f_grossproceeds,SUM(f_benqizhye) f_benqizhye " +
				"from t_sellinggas where f_deliverydate='"+sdf.format(date)+"' "+
				"and f_sgnetwork='"+sgnetwork+"'";
		List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateSQLCall(sql));
		Map<String, Object> map=list.get(0);
		result+="[{f_pregas:"+map.get("f_pregas");
		result+=",f_preamount:"+map.get("f_preamount");
		result+=",f_zhye:"+map.get("f_zhye");
		result+=",f_zhinajin:"+map.get("f_zhinajin");
		result+=",f_grossproceeds:"+map.get("f_grossproceeds");
		result+=",f_benqizhye:"+map.get("f_benqizhye")+"}]";
		return result;
		}catch(Exception e){
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
	}
	
	
	
	// �������أ�����JSON��
	// operator ����Ա������
	@GET
	@Path("{operator}")
	@Produces("application/json")
	public JSONArray ReadRecordInput(@PathParam("operator") String operator) {
		JSONArray array = new JSONArray();
		List<Object> list = this.hibernateTemplate.find(
				"from t_handplan where f_inputtor=? and f_state='δ����'",
				operator);
		for (Object obj : list) {
			// �ѵ���mapת����JSON����
			Map<String, Object> map = (Map<String, Object>) obj;
			JSONObject json = (JSONObject) new JsonTransfer().MapToJson(map);
			array.put(json);
		}
		return array;
	}
	@POST
	@Path("download")
	public String downLoadRecord(String condition) {
		
		String sql = "select top 1000 h.f_userid,h.f_username,h.f_address,u.lastinputgasnum " +
				"from t_handplan h join t_userfiles u on u.f_userid=h.f_userid " +
				"where h.shifoujiaofei='��' and h.f_state='δ����' and u.f_userstate!='����' and " 
				+ condition + " and h.f_userid in (select f_userid from t_userfiles where f_userstate in ('����','���пۿ�'))" +
						" order by h.f_address,h.f_apartment";
		List<Object> list = this.hibernateTemplate.executeFind(new HibernateSQLCall(sql));
		String result="[";
		boolean check=false;
		for (Object obj : list) {
			Map<String, Object> map = (Map<String, Object>) obj;
			if(!result.equals("[")){
				result+=",";
			}
			String item="";
			//�ƻ��·��û�����û�������ַ�ϴε������ε���������
			item+="{";
			item+="f_userid:'"+map.get("f_userid")+"',";
			item+="f_username:'"+map.get("f_username")+"',";
			item+="f_address:'"+map.get("f_address")+"',";
			item+="lastinputgasnum:"+map.get("lastinputgasnum");
			item+="}";
			
			result += item;
		}
		result+="]";
		System.out.println(result);
		return result;
	}
	
	// �������¼��
	// ��������������
	@SuppressWarnings("unchecked")
	@GET
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}")//userid�Ǳ�idҪע��ȡ������id
	@Produces("application/json")
	public String RecordInputForOne(@PathParam("userid") String userid,
			@PathParam("reading") double readingDouble,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate) {

		Map<String,String> singles = getSingles();// ��ȡ���е�ֵ
		BigDecimal reading=new BigDecimal(readingDouble+"");//���ڶ���
		try {
			String hql="";
			final String sql="select " +
			"h.id handId," +
			"isnull(q.c,0) c," +//handplan
			"u.lastinputgasnum lastinputgasnum,u.f_userid f_userid,u.lastrecord lastrecord,u.lastinputdate u_lastinputdate," +//userfiles
			"ui.f_zhye f_zhye,ui.id infoid,ui.f_userid ui_userid,ui.f_username f_username,ui.f_address f_address," +
			"ui.f_districtname f_districtname,ui.f_gaspricetype f_gaspricetype," +
			"ui.f_idnumber f_idnumber,ui.f_gasprice f_gasprice,ui.f_usertype f_usertype," +
			"ui.f_gasproperties f_gasproperties,ui.f_dibaohu f_dibaohu " +//userinfo
			"from (select * from t_handplan where f_state='δ����' and f_userid="+userid+
			") h left join (select f_userid, COUNT(*) c from t_handplan where f_state='�ѳ���' and shifoujiaofei='��' and f_userid=" +userid+
			" group by f_userid) q on h.f_userid=q.f_userid "+
			"join t_userfiles u on h.f_userid=u.f_userid "+
			"join t_userinfo ui on u.f_userinfoid=ui.id and u.f_userstate in ('����','���пۿ�')";
			List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
				public Object doInHibernate(Session session)throws HibernateException {
					Query q = session.createSQLQuery(sql);
					q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					List result = q.list();
					return result;
				}
			});

			// ȡ���ñ��δ�����¼�Լ���������
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			// ���ڶ��������ڵı��γ�����������ڵ�������
			BigDecimal lastReading = new BigDecimal(map.get("lastinputgasnum").toString());
			// ����=����ָ��-�ϴ�ָ��
			BigDecimal gas = reading.subtract(lastReading);
			
			// ����
			String dibaohu=map.get("f_dibaohu").toString();
			//�ͱ����ٽ�����
			BigDecimal boundAmount = new BigDecimal(singles.get("�ͱ����ٽ�����"));
			//�ͱ����ٽ�������
			BigDecimal boundPrice = new BigDecimal(singles.get("�ͱ����ٽ�������"));
			//�ͱ����ٽ�������
			BigDecimal price = new BigDecimal(singles.get("�ͱ�������"));
			//��ͨ��������
			BigDecimal gasPrice = new BigDecimal(map.get("f_gasprice").toString());
			// ������
			BigDecimal amount = null;
			// �ǵͱ���
			if(dibaohu.equals("1")){
				//�����ٽ�����
				if(gas.compareTo(boundAmount)>0){
					//�ٽ�������
					BigDecimal linjiewai=gas.subtract(boundAmount);
					//����=���ٽ�������*�ٽ������ۣ�+���ٽ�����*�ٽ������ۣ�
					amount=(boundPrice.multiply(linjiewai)).add(boundAmount.multiply(price));
				}else{
					amount=price.multiply(gas);//�ٽ�������
				}
			}else{
				amount=gasPrice.multiply(gas);//��ͨ����
			}
			
			
			// �ӻ���ȡ�����(�������)
			BigDecimal f_zhye = new BigDecimal(map.get("f_zhye")+"");
			
			//ȡ��Ƿ������
			int qianfeitiaoshu=Integer.parseInt(map.get("c").toString());
			
			//תΪdouble���ڴ洢
			double gasDoube=gas.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//����
			double amountDouble=amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();//����
			double f_zhyeDouble=f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();//���ڽ���
			double f_zhyeThisDouble=0;//���ڽ���
			double lastinputgasnumDouble=lastReading.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();//�������ڳ������
			
			//ȡϵͳ��ǰ����
			Date now=new Date();
			//���һ�γ�������
			DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
			String dateStr=lastinputdate.substring(0, 10);
			Date lastinputDate=df.parse(dateStr);
			//ȡ���������ڵõ��ɷѽ�ֹ����DateFormat.parse(String s) 
			Date date=endDate(lastinputdate);//�ɷѽ�ֹ����

			// ���>=������ǰ��û��Ƿ�ѣ�������п۳����������Ѽ�¼
			if (f_zhye.compareTo(amount)>=0 && qianfeitiaoshu==0) {
				//���ڽ���
				f_zhyeThisDouble=f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
				
				//��ӽ��Ѽ�¼
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("ui_userid").toString());	// ����id
				sell.put("lastinputgasnum", lastinputgasnumDouble);	// ������ָ��
				sell.put("lastrecord", readingDouble);	// �ܱ���ָ��
				sell.put("f_totalcost", new Double(0));	// Ӧ�����
				sell.put("f_grossproceeds", new Double(0));	// �տ�
				sell.put("f_zhinajin", new Double(0));	// ���ɽ�
				sell.put("f_deliverydate", now);	// ��������
				sell.put("f_deliverytime", now);	// ����ʱ��
				sell.put("f_zhye", f_zhyeDouble);		//���ڽ���
				sell.put("f_benqizhye", f_zhyeThisDouble);	//���ڽ���
				sell.put("f_gasmeterstyle", "����");	//��������
				sell.put("f_comtype", "��Ȼ����˾");	//��˾���ͣ���Ϊ��Ȼ����˾������
				sell.put("f_username", map.get("f_username"));		//�û�/��λ����
				sell.put("f_address", map.get("f_address"));	//��ַ
				sell.put("f_districtname", map.get("f_districtname"));	//С������
				sell.put("f_idnumber",  map.get("f_idnumber"));	//���֤��
				sell.put("f_gaswatchbrand", "����");	//����Ʒ��
				sell.put("f_gaspricetype", map.get("f_gaspricetype"));	//��������
				sell.put("f_gasprice", map.get("f_gasprice")); //����
				sell.put("f_usertype", map.get("f_usertype"));	//�û�����
				sell.put("f_gasproperties", map.get("f_gasproperties")); //��������
				sell.put("f_pregas", gasDoube);	//����
				sell.put("f_preamount", amountDouble);	//����
				sell.put("f_payment", "�ֽ�");	//���ʽ
				sell.put("f_paytype", "�ֽ�");	//�������ͣ����д���/�ֽ�
				sell.put("f_sgnetwork", sgnetwork);	//����
				sell.put("f_sgoperator", sgoperator);	//�� �� Ա
				sell.put("f_filiale", "��˳��ܵ���Ȼ�����޹�˾");	//�ֹ�˾
				sell.put("f_fengongsinum", "11");	//�ֹ�˾���
				sell.put("f_payfeetype", "��潻��");	//��������
				sell.put("f_payfeevalid", "��Ч");	//������Ч����
				//sell.put("f_useful",map.get("handId").toString() );	//�����¼id
				sell.put("f_users", userid);	//�����¼�û�id
				hibernateTemplate.save("t_sellinggas", sell);
				 final String Sql = "select id from t_sellinggas where f_userid ='" + userid +"' order by f_deliverydate desc";
					List<Map<String, Object>> sellList =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
						public Object doInHibernate(Session session)throws HibernateException {
							Query q = session.createSQLQuery(Sql);
							q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
							List result = q.list();
							return result;
						}
					});
			// ȡ��δ�����¼�Լ��û���Ϣ
			Map<String, Object> handmap = (Map<String, Object>) sellList.get(0);
			String sellId = handmap.get("id").toString();
				
				// ���»������
				hibernateTemplate.bulkUpdate(
						"update t_userinfo set f_zhye=? where id=?",
						new Object[]{ f_zhyeThisDouble, map.get("infoid")});
				// �����û�����							�ϴγ������
				hql = "update t_userfiles set lastinputgasnum=?,"+
					//��󳭱�����(Ӧ���ǳ���������)
					"lastinputdate=?, "+
					//��ǰ���ۼƹ����� 						  ���ۼƹ�����
					"f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"+
					// ������� 			��������� 			�����ʱ��
					"f_finallybought=?, f_finabuygasdate=?, f_finabuygastime=? "+
					"where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {readingDouble,lastinputDate,gasDoube,
						gasDoube,gasDoube,now,now, userid });
				// ���³����¼��״̬				����״̬			�շ�״̬
				hql = "update t_handplan set f_state ='�ѳ���',shifoujiaofei='��'," +
					//�ϴγ�������					����ָ��	   ����Ա			����			¼������
						"scinputdate=?,lastinputgasnum=?,f_operator=?,f_network=?,f_inputdate=?," +
						//����ָ��			������		��������		���γ�������
						"lastrecord=?,oughtamount=?,oughtfee=?,f_address=?,f_username=?,lastinputdate=?,f_sellid=?, " +
						//���ѽ�ֹ����
						"f_endjfdate=? " +
						"where f_userid=? and f_state='δ����'";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						map.get("u_lastinputdate"),lastinputgasnumDouble,sgoperator,sgnetwork,now,
						readingDouble,gasDoube,amountDouble,map.get("f_address"),map.get("f_username"),lastinputDate,sellId,date,userid });
				return "";
			} else {
				// �����û�����							�ϴγ������
				hql = "update t_userfiles set lastinputgasnum=?,"+
					//��󳭱�����(Ӧ���ǳ���ʱ����)
					"lastinputdate=?, "+
					// ��ǰ���ۼƹ����� 						���ۼƹ�����
					"f_metergasnums?, f_cumulativepurchase=? ,"+
					// ������� 			��������� 			�����ʱ��
					"f_finallybought=?, f_finabuygasdate=?, f_finabuygastime=? "+
					"where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						readingDouble,lastinputDate,gasDoube,gasDoube,gasDoube,
						now,now,userid });
				// Ƿ��,���³����¼��״̬f_state����������
				hql = "update t_handplan set f_state ='�ѳ���',shifoujiaofei='��'," +
				//�ϴγ�������				����ָ��	   ����Ա			����			¼������
				"scinputdate=?,lastinputgasnum=?,f_operator=?,f_network=?,f_inputdate=?," +
				//����ָ��			������		��������		���γ�������
				"lastrecord=?,oughtamount=?,oughtfee=?,f_address=?,f_username=?,lastinputdate=?," +
				// ���ѽ�ֹ�·�
				"f_endjfdate=? " +
				"where f_userid=? and f_state='δ����'";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						map.get("u_lastinputdate"),lastinputgasnumDouble,sgoperator,sgnetwork,now,
						readingDouble,gasDoube,amountDouble,map.get("f_address"),map.get("f_username"),lastinputDate,date,userid });
				return "";
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
	}
	
	// ��ȡ���е�ֵ��ת����Map
	private Map<String, String> getSingles() {
		Map result = new HashMap<String, String>();
		
		String sql = "select name,value from t_singlevalue";
		List<Map<String, Object>> list = this.hibernateTemplate.executeFind(new HibernateSQLCall(sql));
		for (Map<String, Object> hand : list) {
			result.put(hand.get("name"), hand.get("value"));
		}
		return result;
	}
	
	
	/**
	 * �����ɷѼ�¼
	 * 
	 * @param userid
	 *            �û�id
	 * @param amount
	 *            ����
	 * @param payment
	 *            �ɷѶ�
	 */
	private void InsertSellingRecord(String userid, double amount,
			double payment) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("f_userid", userid);
		map.put("f_myqiliang", new Double(amount));
		map.put("f_myqiliangjine", new Double(payment));
		// TODO �����ֶ���ô��
		this.hibernateTemplate.saveOrUpdate("t_sellinggas", map);
		this.hibernateTemplate.flush();
	}

	/**
	 * ������
	 * 
	 * @param userid
	 *            �û�id
	 * @param amount
	 *            ����
	 * @param userType
	 *            �û����ͣ����á������ã�
	 * @param isLowIncome
	 *            �Ƿ�ͱ���
	 * @param nonCivilPrice
	 *            ����������
	 * @return Ӧ���ѽ��
	 */
	private double getAmount(String userid, int amount, String userType,
			boolean isLowIncome, double nonCivilPrice) {
		if (userType.equals("����")) {
			int boundAmount;
			double boundPrice;
			double price;
			if (isLowIncome) {
				
				boundAmount = Integer.parseInt(getSingleValue("�ͱ����ٽ�����"));
				boundPrice = Double.parseDouble(getSingleValue("�ͱ����ٽ�������"));
				price = Double.parseDouble(getSingleValue("�ͱ�������"));
			} else {
				boundAmount = Integer.parseInt(getSingleValue("�����ٽ�����"));
				boundPrice = Double.parseDouble(getSingleValue("�ٽ�������"));
				price = Double.parseDouble(getSingleValue("��������"));
			}
			if (boundAmount >= amount)
				return amount * price;
			else
				return amount * price + (amount - boundAmount) * boundPrice;
		}
		// ������
		else
			return amount * nonCivilPrice;
	}

	/**
	 * ȡ��ֵ
	 * 
	 * @param name
	 *            ��ֵ��
	 * @return ��ֵֵ
	 */
	private String getSingleValue(String name) {
		final String sql = "select value from t_singlevalue where name='"
				+ name + "'";
		String value = (String) hibernateTemplate
				.execute(new HibernateCallback() {
					public Object doInHibernate(Session session)
							throws HibernateException {
						SQLQuery query = session.createSQLQuery(sql);
						// addScalar ��ʽָ���������ݵ�����
						query.addScalar("value", Hibernate.STRING);
						return query.uniqueResult();
					}
				});
		return value;
	}

	// ���������¼�ϴ�
	// data��JSON��ʽ�ϴ���[{userid:'�û����', showNumber:���ڳ�����},{}]
	@Path("record/batch/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate) {
		try {
			// ȡ������Ա
			// String operator = dmp.getString("operator");
			// ȡ����������
			JSONArray rows = new JSONArray(data);
			// ��ÿһ�����ݣ����õ����������ݴ������
			JSONObject row =null;
			for (int i = 0; i < rows.length(); i++) {
				row = rows.getJSONObject(i);
				String userid = row.getString("userid");
				double showNumber = row.getDouble("reading");
				RecordInputForOne(userid,showNumber,sgnetwork,sgoperator,lastinputdate);
//				if (result.getString("ok").equals("nok"))
//					return result.toString();
//				// ���Ƿ��
//				else if (result.has("err")) {
//					JSONObject err = new JSONObject(result.getString("err"));
//					errs.put(err);
//				}
			}
//			return "{\"ok\":\"ok\",\"err\":" + errs.toString() + "}";
		} catch (Exception e) {
			return "{\"ok\":\"nok\", msg:\"" + e.toString() + "\"}";
		}
		return "";
	}

	//�������ѽ�ֹ����
	private Date endDate(String str) throws ParseException {
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		String dateStr=str.substring(0, 10);
		Date now=df.parse(dateStr);
		Calendar c=Calendar.getInstance();
		c.setTime(now);
		c.add(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 20);
		return c.getTime();
	}	

	// ת��������ת���ڼ��������Ƿ��Ѿ�ת��������������ת����������ѭ��
	class JsonTransfer {
		// �����Ѿ�ת�����Ķ���
		private List<Map<String, Object>> transed = new ArrayList<Map<String, Object>>();

		// �ѵ���mapת����JSON����
		public Object MapToJson(Map<String, Object> map) {
			// ת���������ؿն���
			if (contains(map))
				return JSONObject.NULL;
			transed.add(map);
			JSONObject json = new JSONObject();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				try {
					String key = entry.getKey();
					Object value = entry.getValue();
					// ��ֵת����JSON�Ŀն���
					if (value == null) {
						value = JSONObject.NULL;
					} else if (value instanceof HashMap) {
						value = MapToJson((Map<String, Object>) value);
					}
					// �����$type$����ʾʵ�����ͣ�ת����EntityType
					if (key.equals("$type$")) {
						json.put("EntityType", value);
					} else if (value instanceof Date) {
						Date d1 = (Date) value;
						Calendar c = Calendar.getInstance();
						long time = d1.getTime() + c.get(Calendar.ZONE_OFFSET);
						json.put(key, time);
					} else if (value instanceof MapProxy) {
						// MapProxyû�м��أ�����
					} else if (value instanceof PersistentSet) {
						PersistentSet set = (PersistentSet) value;
						// û���صļ��ϲ���
						if (set.wasInitialized()) {
							json.put(key, ToJson(set));
						}
					} else {
						json.put(key, value);
					}
				} catch (JSONException e) {
					throw new WebApplicationException(400);
				}
			}
			return json;
		}

		// �Ѽ���ת����Json����
		public Object ToJson(PersistentSet set) {
			JSONArray array = new JSONArray();
			for (Object obj : set) {
				Map<String, Object> map = (Map<String, Object>) obj;
				JSONObject json = (JSONObject) MapToJson(map);
				array.put(json);
			}
			return array;
		}

		// �ж��Ѿ�ת�������������Ƿ������������
		public boolean contains(Map<String, Object> obj) {
			for (Map<String, Object> map : this.transed) {
				if (obj == map) {
					return true;
				}
			}
			return false;
		}
	}
	class HibernateSQLCall implements HibernateCallback {
		String sql;

		public HibernateSQLCall(String sql) {
			this.sql = sql;
		}

		public Object doInHibernate(Session session) {
			Query q = session.createSQLQuery(sql);
			q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
			List result = q.list();
			return result;
		}
	}

}
