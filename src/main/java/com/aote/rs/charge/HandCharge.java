package com.aote.rs.charge;

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

import com.aote.rs.charge.HandCharge.HibernateSQLCall;

@Path("handcharge")
@Component
public class HandCharge {

	static Logger log = Logger.getLogger(HandCharge.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;


	private int stairmonths;
	
	private String stardate;
	private String enddate;
	
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
	//��ѯ��������	
    @POST
	@Path("download")
	public String downLoadRecord(String condition) {
		
		String sql = "select top 1000 u.f_userid,u.f_username,u.f_address,u.lastinputgasnum " +
				"from t_handplan h left join t_userfiles u on h.f_userid = u.f_userid where h.shifoujiaofei='��' and u.f_userstate!='ע��' and h.f_state='δ����' and " 
				+ condition + "	order by u.f_address,u.f_apartment";
		List<Object> list = this.hibernateTemplate.executeFind(new HibernateSQLCall(sql));
//		String result="[";
		boolean check=false;
		JSONArray array = new JSONArray();
		for (Object obj : list) {
			JSONObject json = (JSONObject) new JsonTransfer().MapToJson((Map<String, Object>)obj);
//			Map<String, Object> map = (Map<String, Object>) json;
//			if(!result.equals("[")){
//				result+=",";
//			}
//			String item="";
//			//�ƻ��·��û�����û�������ַ�ϴε������ε���������
//			item+="{";
//			item+="f_userid:'"+map.get("f_userid")+"',";
//			item+="f_username:'"+map.get("f_username")+"',";
//			item+="f_address:'"+map.get("f_address")+"',";
//			item+="lastinputgasnum:"+map.get("lastinputgasnum");
//			item+="}";
//			
//			result += item;
			array.put(json);
		}
//		result+="]";
//		System.out.println(result);
		
		return array.toString();
	}

	// �������¼��
	// ��������������
	@SuppressWarnings("unchecked")
	@GET
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}/{handdate}")
	@Produces("application/json")
	public String RecordInputForOne(
			@PathParam("userid") String userid,
			@PathParam("reading") double reading,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate) {
		try {
			return recordInput(userid, reading, sgnetwork, sgoperator,
					lastinputdate, handdate, 0);
		} catch (Exception e) {
			log.debug(e.getMessage());
			return e.getMessage();
		}
	}
	
	// �������¼����ڲ�������֧�ֿ������������¼����������
	private String recordInput(String userid, double reading, String sgnetwork,
			String sgoperator, String lastinputdate, String handdate, double leftgas)
			throws ParseException {
		Map<String,String> singles = getSingles();// ��ȡ���е�ֵ
		double chargenum = 0;
		double stair1num = 0;
		double stair2num = 0;
		double stair3num = 0;
		double stair4num = 0;
		double stair1fee = 0;
		double stair2fee = 0;
		double stair3fee = 0;
		double stair4fee = 0;
		double sumamont = 0;
		String hql = "";
		final String sql = "select isnull(u.f_userid,'') f_userid, isnull(u.f_zhye,'') f_zhye , isnull(u.lastinputgasnum,'') lastinputgasnum, isnull(u.f_gasprice,0) f_gasprice, isnull(u.f_username,'')  f_username,"
				+ "isnull(u.f_stair1amount,0)f_stair1amount,isnull(u.f_stair2amount,0)f_stair2amount,isnull(u.f_stair3amount,0)f_stair3amount,isnull(u.f_stair1price,0)f_stair1price,isnull(u.f_stair2price,0)f_stair2price,isnull(u.f_stair3price,0)f_stair3price,isnull(u.f_stair4price,0)f_stair4price,isnull(u.f_stairmonths,0)f_stairmonths,isnull(u.f_stairtype,'δ��')f_stairtype,"
				+ "isnull(u.f_address,'')f_address ,isnull(u.f_districtname,'')f_districtname,isnull(u.f_cusDom,'')f_cusDom,isnull(u.f_cusDy,'')f_cusDy,isnull(u.f_gasmeterstyle,'') f_gasmeterstyle, isnull(u.f_idnumber,'') f_idnumber, isnull(u.f_gaswatchbrand,'')f_gaswatchbrand, isnull(u.f_usertype,'')f_usertype, "
				+ "isnull(u.f_gasproperties,'')f_gasproperties,isnull(u.f_dibaohu,0)f_dibaohu,isnull(u.f_payment,'')f_payment,isnull(u.f_zerenbumen,'')f_zerenbumen,isnull(u.f_menzhan,'')f_menzhan,isnull(u.f_inputtor,'')f_inputtor, isnull(q.c,0) c,"
				+ "isnull(u.f_metergasnums,0) f_metergasnums,isnull(u.f_cumulativepurchase,0)f_cumulativepurchase, "
				+ "isnull(u.f_finallybought,0)f_finallybought,isnull(u.f_cardid,'NULL') f_cardid,isnull(u.f_filiale,'NULL')f_filiale,"
				+ "h.id id from (select * from t_handplan where f_state='δ����' and f_userid='"
				+ userid
				+ "') h "
				+ "left join (select f_userid, COUNT(*) c from t_handplan where f_state='�ѳ���' and shifoujiaofei='��' "
				+ "group by f_userid) q on h.f_userid=q.f_userid join t_userfiles u on h.f_userid=u.f_userid";
		List<Map<String, Object>> list =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session)throws HibernateException {
				Query q = session.createSQLQuery(sql);
				q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List result = q.list();
				return result;
			}
		});
		// ȡ��δ�����¼�Լ�����
		Map<String, Object> map = (Map<String, Object>) list.get(0);
		double gasprice = Double.parseDouble(map.get("f_gasprice").toString());
		String stairtype = map.get("f_stairtype").toString();
		double stair1amount = Double.parseDouble(map.get("f_stair1amount").toString());
		double stair2amount = Double.parseDouble(map.get("f_stair2amount").toString());
		double  stair3amount = Double.parseDouble(map.get("f_stair3amount").toString());
		double stair1price = Double.parseDouble(map.get("f_stair1price").toString());
		double stair2price = Double.parseDouble(map.get("f_stair2price").toString());
		double stair3price = Double.parseDouble(map.get("f_stair3price").toString());
		double stair4price = Double.parseDouble(map.get("f_stair4price").toString());
		stairmonths = Integer.parseInt(map.get("f_stairmonths").toString());
		// ���ڶ��������ڵı��γ�����������ڵ�������
		double lastReading = Double.parseDouble(map.get("lastinputgasnum")+ "");
		// ����
		double gas = reading-lastReading;
		// �ӻ���ȡ�����(�������)
		double f_zhye = Double.parseDouble(map.get("f_zhye") + "");
		//�û���ַ
		String address = map.get("f_address").toString();
		//�û�����
		String username = map.get("f_username").toString();
		// ��ǰǷ������
		int items = Integer.parseInt(map.get("c") + "");
		//����id
		String handid = map.get("id") + "";
		// �û��ɷ�����
		String payment = map.get("f_payment").toString();
		// ���β���
		String zerenbumen ="��";
		// ��վ
		String menzhan ="��";
		// ����Ա
		String inputtor = map.get("f_inputtor")+"";
		//���һ�γ�������
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		String dateStr=lastinputdate.substring(0, 10);
		Date lastinputDate=df.parse(dateStr);
		//ȡ���������ڵõ��ɷѽ�ֹ����DateFormat.parse(String s) 
		Date date=endDate(lastinputdate);//�ɷѽ�ֹ����
		//¼������  
		Date inputdate=new Date();
		//�ƻ��·�
		DateFormat hd=new SimpleDateFormat("yyyy-MM");
		String dateStr1=handdate.substring(0, 7);
		Date handDate=hd.parse(dateStr1);
		//��ǰ���ۼƹ����� ���ݣ�
		double f_metergasnums =Double.parseDouble(map.get("f_metergasnums")+""); 
		//f_cumulativepurchase ���ۼƹ�����
		double f_cumulativepurchase =Double.parseDouble(map.get("f_cumulativepurchase")+"");
		//������ý������۵��û�����
		CountDate();
		if(!stairtype.equals("δ��")){
	        final String gassql = " select isnull(sum(oughtamount),0)oughtamount from t_handplan " +
	        		"where f_userid='"+userid+"' and lastinputdate>='"+stardate+"' and lastinputdate<='"+enddate+"'";
			List<Map<String, Object>> gaslist =(List<Map<String, Object>>) hibernateTemplate.execute(new HibernateCallback() {
				public Object doInHibernate(Session session)throws HibernateException {
					Query q = session.createSQLQuery(gassql);
					q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					List result = q.list();
					return result;
				}
			});
			Map<String, Object> gasmap = (Map<String, Object>) gaslist.get(0);
			//��ǰ������
			sumamont = Double.parseDouble(gasmap.get("oughtamount").toString());
			//�ۼƹ�����
			double allamont = sumamont+gas;
			//��ǰ�������ڵ�һ����
			if(sumamont<stair1amount){
				if(allamont<stair1amount){
					stair1num = gas;
					stair1fee = gas*stair1price;
					chargenum = gas*stair1price;
				}else if(allamont>=stair1amount && allamont<stair2amount){
					stair1num = stair1amount-sumamont;
					stair1fee = (stair1amount-sumamont)*stair1price;
					stair2num = allamont-stair1amount;
					stair2fee = (allamont-stair1amount)*stair2price;
					chargenum = stair1fee+stair2fee;
				}else if(allamont>=stair2amount && allamont<stair3amount){
					stair1num = stair1amount-sumamont;
					stair1fee = (stair1amount-sumamont)*stair1price;
					stair2num = stair2amount-stair1amount;
					stair2fee = (stair2amount-stair1amount)*stair2price;
					stair3num = allamont-stair2amount;
					stair3fee = (allamont-stair2amount)*stair3price;
					chargenum = stair1fee+stair2fee+stair3fee;
				}else if(allamont>=stair3amount){
					stair1num = stair1amount-sumamont;
					stair1fee = (stair1amount-sumamont)*stair1price;
					stair2num = stair2amount-stair1amount;
					stair2fee = (stair2amount-stair1amount)*stair2price;
					stair3num = stair3amount-stair2amount;
					stair3fee = (stair3amount-stair2amount)*stair3price;
					stair4num = allamont-stair3amount;
					stair4fee = (allamont-stair3amount)*stair4price;
					chargenum = stair1fee+stair2fee+stair3fee+stair4fee;
				}
			//��ǰ�ѹ������ڽ��ݶ���
			}else if(sumamont>=stair1amount && sumamont<stair2amount){
				if(allamont<stair2amount){
					stair2num = gas;
					stair2fee = gas*stair2price;
					chargenum = stair2fee;
				}else if(allamont>=stair2amount && allamont<stair3amount){
					stair2num = stair2amount-sumamont;
					stair2fee = (stair2amount-sumamont)*stair2price;
					stair3num = allamont-stair2amount;
					stair3fee = (allamont-stair2amount)*stair3price;
					chargenum = stair2fee+stair3fee;
				}else{
					stair2num = stair2amount-sumamont;
					stair2fee = (stair2amount-sumamont)*stair2price;
					stair3num = stair3amount-stair2amount;
					stair3fee = (stair3amount-stair2amount)*stair3price;
					stair4num = allamont-stair3amount;
					stair4fee = (allamont-stair3amount)*stair4price;
					chargenum = stair2fee+stair3fee+stair4fee;
				}
			//��ǰ�ѹ������ڽ�������
			}else if(sumamont>=stair2amount && sumamont<stair3amount){
				if(allamont<stair3amount){
					stair3num = gas;
					stair3fee = gas*stair3price;
					chargenum = stair3fee;
				}else{
					stair3num = stair3amount-sumamont;
					stair3fee = (stair3amount-sumamont)*stair3price;
					stair4num = allamont-stair3amount;
					stair4fee = (allamont-stair3amount)*stair4price;
					chargenum = stair3fee+stair4fee;
				}
			//��ǰ�ѹ���������������
			}else if(sumamont>=stair3amount){
				stair4num = gas;
				stair4fee = gas*stair4price;
				chargenum =	stair4fee;
			}
		//���û�δ���ý�������
		}else{
			chargenum = gas*gasprice;
			stair1num = 0;
			stair2num = 0;
			stair3num = 0;
			stair4num = 0;
			stair1fee = 0;
			stair2fee = 0;
			stair3fee = 0;
			stair4fee = 0;
		}
		if(chargenum<f_zhye && items<1){
			//�Զ�����
			Map<String, Object> sell = new HashMap<String, Object>();
			sell.put("f_userid", map.get("f_userid")); // �û�ID
			sell.put("f_payfeevalid", "��Ч");// �����Ƿ���Ч
			sell.put("f_payfeetype", "�Զ�����");// �շ�����
			sell.put("lastinputgasnum", lastReading); // ���ڵ���
			sell.put("lastrecord", reading); // ���ڵ���
			sell.put("f_totalcost", chargenum); // Ӧ�����
			sell.put("f_grossproceeds", chargenum); // �տ�
			sell.put("f_deliverydate", new Date()); // ��������
			sell.put("f_zhye", f_zhye); // ���ڽ���
			sell.put("f_benqizhye", f_zhye-chargenum); // ���ڽ���
			sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // ��������
			sell.put("f_comtype", "��Ȼ����˾"); // ��˾���ͣ���Ϊ��Ȼ����˾������
			sell.put("f_username", map.get("f_username")); // �û�/��λ����
			sell.put("f_address", map.get("f_address")); // ��ַ
			sell.put("f_districtname", map.get("f_districtname")); // С������
			sell.put("f_cusDom", map.get("f_cusDom")); // ¥��
			sell.put("f_cusDy", map.get("f_cusDy")); // ��Ԫ
			sell.put("f_idnumber", map.get("f_idnumber")); // ���֤��
			sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // ����Ʒ��
			sell.put("f_gaspricetype", map.get("f_gaspricetype")); // ��������
			sell.put("f_gasprice", gasprice); // ����
			sell.put("f_usertype", map.get("f_usertype")); // �û�����
			sell.put("f_gasproperties", map.get("f_gasproperties")); // ��������
			sell.put("f_pregas", gas); // ����
			sell.put("f_preamount", chargenum); // ���
			sell.put("f_payment", "�ֽ�"); // ���ʽ
			sell.put("f_sgnetwork", sgnetwork); // ����
			sell.put("f_sgoperator", sgoperator); // �� �� Ա
			sell.put("f_cardid", map.get("f_cardid"));//����
			sell.put("f_filiale", map.get("f_filiale")); // �ֹ�˾
			sell.put("f_useful", handid); // ����id
			sell.put("f_stair1amount", stair1num);
			sell.put("f_stair2amount", stair2num);
			sell.put("f_stair3amount", stair3num);
			sell.put("f_stair4amount", stair4num);
			sell.put("f_stair1fee", stair1fee);
			sell.put("f_stair2fee", stair2fee);
			sell.put("f_stair3fee", stair3fee);
			sell.put("f_stair4fee", stair4fee);
			sell.put("f_stair1price", stair1price);
			sell.put("f_stair2price", stair2price);
			sell.put("f_stair3price", stair3price);
			sell.put("f_stair4price", stair4price);
			sell.put("f_stardate", stardate);
			sell.put("f_enddate", enddate);
			sell.put("f_allamont", sumamont);
			int sellid = (Integer)hibernateTemplate.save("t_sellinggas", sell);
			hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
			// ���γ�������
					"  lastinputdate=? " +
					// ��ǰ���ۼƹ����� ���ݣ� ���ۼƹ�����
					 ",f_metergasnums=  ?, f_cumulativepurchase= ? ,"
					// ������� ��������� �����ʱ��
					+ "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
					+ "where f_userid=?";

			hibernateTemplate.bulkUpdate(hql, new Object[] {
					f_zhye-chargenum,reading, lastinputDate,f_metergasnums+gas,f_cumulativepurchase+gas,gas,inputdate,inputdate,userid });
			String sellId = sellid+"";
			// ���³����¼
			hql = "update t_handplan set f_state='�ѳ���',shifoujiaofei='��',f_handdate=?,f_stairtype=?,"
					+ "lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?,lastrecord=? ,oughtamount=? ,oughtfee=? ,f_address=?, f_username=?, f_zhye=?, f_bczhye=?,"
					+ "f_stair1amount=?,f_stair2amount=?,f_stair3amount=?,f_stair4amount=?,f_stair1fee=?,f_stair2fee=?,f_stair3fee=?,f_stair4fee=?,f_stair1price=?,f_stair2price=?,f_stair3price=?,f_stair4price=?,"
					+ "f_stardate=?,f_enddate=?,f_allamont=? ,f_sellid=?, f_leftgas=? , f_inputdate=?,f_network=?,f_operator=?  "
					+ "where f_userid=? and f_state='δ����'";
			hibernateTemplate.bulkUpdate(hql, new Object[] { handDate,stairtype,
					lastinputDate,zerenbumen, menzhan, inputtor,reading,
					gas,chargenum, address, username,f_zhye,f_zhye-chargenum,
					stair1num,stair2num,stair3num,stair4num,
					stair1fee,stair2fee,stair3fee,stair4fee,
					stair1price,stair2price,stair3price,stair4price,
					stardate,enddate,sumamont,sellId, leftgas,inputdate,sgnetwork,sgoperator,
					userid });
		}else{
			// �����û�����
			hql = "update t_userfiles " +
			// ���γ������ ���γ�������
					"set lastinputgasnum=? ,  lastinputdate=?  where f_userid=?";
			hibernateTemplate.bulkUpdate(hql, new Object[] {reading, lastinputDate, userid });

			// Ƿ��,���³����¼��״̬f_state���������ڡ����γ������
			hql = "update t_handplan set f_state ='�ѳ���', shifoujiaofei='��',f_handdate=?,"
				+ "lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?, lastrecord=? ,f_stairtype=?," 
				+ "oughtamount=?,  f_endjfdate=? , oughtfee=?, f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?,"
				+ "f_stair1amount=?,f_stair2amount=?,f_stair3amount=?,f_stair4amount=?,f_stair1fee=?,f_stair2fee=?,f_stair3fee=?,f_stair4fee=?," 
				+ "f_stair1price=?,f_stair2price=?,f_stair3price=?,f_stair4price=?,"
				+ "f_stardate=?,f_enddate=?,f_allamont=?, f_leftgas=? "
				+ "where f_userid=? and f_state='δ����'";
		hibernateTemplate.bulkUpdate(hql, new Object[] { handDate,lastinputDate,
				zerenbumen, menzhan, inputtor,reading,stairtype,gas,date,
				chargenum,inputdate,sgnetwork,sgoperator, address, username,
				stair1num,stair2num,stair3num,stair4num,
				stair1fee,stair2fee,stair3fee,stair4fee,
				stair1price,stair2price,stair3price,stair4price,
				stardate,enddate,sumamont,leftgas,
				userid });
		}
		return "";
	}
	
	//���㿪ʼʱ�䷽��
	private	void CountDate(){
		//���㵱ǰ�����ĸ���������
		Calendar cal = Calendar.getInstance();
		int thismonth = cal.get(Calendar.MONTH) + 1;
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		if(stairmonths==1){
			cal.set(Calendar.DAY_OF_MONTH,1);
	        stardate = format.format(cal.getTime());
	        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));  
	        enddate = format.format(cal.getTime());
		}else if(stairmonths==0){
			stardate ="";
			enddate = "";
		}else{
			/*������ʼ��������
			 *��ʼ�� = ��ǰ��/��������*��������+1
			 *������ = ��ǰ��/��������*��������+��������
			 *ע�������� ��ǰ����12��ʱ����Ҫ��1 �����Ѿ������������Ϊ1����ʱ�Ľ��
			 *һ�����������������Ϊ������  ���Զ�������û��Ӱ��
			 * */
			if(thismonth==12){
				thismonth=11;
			}
			//������ʼ��
			int star = Math.round(thismonth/stairmonths)*stairmonths+1;
			//���������
			int end = Math.round(thismonth/stairmonths)*stairmonths+stairmonths;
			//�����ʼ���ںͽ�������
	        cal.set(Calendar.MONTH, star-1);
	        cal.set(Calendar.DAY_OF_MONTH,1);
	        stardate = format.format(cal.getTime());
		    cal.set(Calendar.MONTH, end-1);
	        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
	        enddate = format.format(cal.getTime());
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
	private Object String(String zerenbumen) {
		// TODO Auto-generated method stub
		return null;
	}



	// ���������¼�ϴ�
	// data��JSON��ʽ�ϴ���[{userid:'�û����', showNumber:���ڳ�����},{}]
	@Path("record/batch/{handdate}/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate)  {
		try {
			// ȡ����������
			JSONArray rows = new JSONArray(data);
			// ��ÿһ�����ݣ����õ����������ݴ������
			for (int i = 0; i < rows.length(); i++) {
				JSONObject row = rows.getJSONObject(i);
				String userid = row.getString("userid");
				double reading = row.getDouble("reading");
				
				//��ȡ������������¼�룬û������������Double.NaN
				double leftgas = 0;
				if(row.has("leftgas")) {
					leftgas = row.getDouble("leftgas");
				}
				
				//BigDecimal readingThis = new BigDecimal(reading + "");
				recordInput(userid, reading, sgnetwork, sgoperator,lastinputdate,handdate, leftgas);
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return "";
	}
	private String SolitaryCopyMeter(){
		return null;
		
	}
	//�������ѽ�ֹ����
	private Date endDate(String str) throws ParseException {
		DateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		String dateStr=str.substring(0, 10);
		Date now=df.parse(dateStr);
		Calendar c=Calendar.getInstance();
		c.setTime(now);
		c.add(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 10);
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
