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

import com.aote.rs.HandService.HibernateSQLCall;

@Path("hand")
@Component
public class HandService {

	static Logger log = Logger.getLogger(HandService.class);

	@Autowired
	private HibernateTemplate hibernateTemplate;

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
	
	String sql = "select top 1000 u.f_userid,u.f_username,u.f_districtname,u.f_address,u.lastinputgasnum " +
			"from t_handplan h left join t_userfiles u on h.f_userid = u.f_userid where h.shifoujiaofei='��' and u.f_userstate!='ע��' and h.f_state='δ����' and " 
			+ condition + "	order by u.f_address,u.f_apartment";
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
		item+="f_districtname:'"+map.get("f_districtname")+"',";
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
	@Path("record/one/{userid}/{reading}/{sgnetwork}/{sgoperator}/{lastinputdate}/{handdate}")
	@Produces("application/json")
	public String RecordInputForOne(
			@PathParam("userid") String userid,
			@PathParam("reading") double reading,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdate") String handdate) {
		Map<String,String> singles = getSingles();// ��ȡ���е�ֵ
		try {
			String hql = "";
			final String sql = "select u.f_userid f_userid, u.f_zhye f_zhye , u.lastinputgasnum lastinputgasnum, u.f_gasprice , u.f_username  f_username,"
					+ "u.f_address f_address,u.f_districtname f_districtname,u.f_gasmeterstyle f_gasmeterstyle, u.f_idnumber f_idnumber, u.f_gaswatchbrand f_gaswatchbrand, u.f_usertype f_usertype, "
					+ "u.f_gasproperties f_gasproperties,u.f_dibaohu f_dibaohu, u.f_payment f_payment,u.f_zerenbumen f_zerenbumen,u.f_menzhan f_menzhan,u.f_inputtor f_inputtor, isnull(q.c,0) c,"
					+ "u.lastinputgasnum lastinputgasnum, h.id id from (select * from t_handplan where f_state='δ����' and f_userid='"
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
			// ���ڶ��������ڵı��γ�����������ڵ�������
			BigDecimal lastReading = new BigDecimal(map.get("lastinputgasnum")+ "");
			// �����ζ���ת��BigDecimal
			BigDecimal readingThis = new BigDecimal(reading + "");
			// �ͱ���
			String dibaohu=map.get("f_dibaohu")+"";
			//�ͱ����ٽ�����
			BigDecimal poorBorderAmount = new BigDecimal(singles.get("�ͱ����ٽ�����"));
			//�����ٽ�����
			BigDecimal borderAmount = new BigDecimal(singles.get("�����ٽ�����"));
			//�ͱ����ٽ�������
			BigDecimal poorOverPrice = new BigDecimal(singles.get("�ͱ����ٽ�������"));
			//�ͱ����ٽ�������
			BigDecimal poorPrice = new BigDecimal(singles.get("�ͱ�������"));
			//��ͨ�����ٽ�������
			BigDecimal overPrice = new BigDecimal(singles.get("�ٽ�������"));
			//��ͨ�����ٽ�������
			BigDecimal gasPrice = new BigDecimal(singles.get("��������"));
			// ������
			BigDecimal amount = null;
			// ����
			BigDecimal gas = readingThis.subtract(lastReading);
			// �ǵͱ���
			if(dibaohu.equals("1")){
				//���ڵͱ����ٽ�����
				if(gas.compareTo(poorBorderAmount)>0 && gas.compareTo(borderAmount)<=0){
					//�ٽ�������(������ڵͱ����ٽ������Ĳ���)
					BigDecimal multiOverPoorBorderAmount=gas.subtract(poorBorderAmount);
					//����=���ͱ����ٽ�����*�ͱ����ٽ������ۣ�+(�ٽ�������*�ͱ����ٽ�������)
					amount=(poorBorderAmount.multiply(poorPrice)).add(multiOverPoorBorderAmount.multiply(poorOverPrice));
				}
				//�����������������ٽ�����
				else if(gas.compareTo(borderAmount)>0){
					//�ٽ�������(������������ٽ������Ĳ���)
					BigDecimal multiOverBorderAmount=gas.subtract(borderAmount);
					//����=((�����ٽ�����-�ͱ����ٽ�����)*�ٽ������ۣ�+���ͱ����ٽ�����*�ͱ����ٽ������ۣ�+(�ٽ�������*��������)
					amount=((borderAmount.subtract(poorBorderAmount)).multiply(poorOverPrice)).add(poorBorderAmount.multiply(poorPrice)).add(multiOverBorderAmount.multiply(gasPrice));
				}
				else{
					amount=poorPrice.multiply(gas);//�ٽ�������
				}
			//���ǵͱ���
			}else{
				//�����ٽ�����	
				if(gas.compareTo(borderAmount)>0){
					BigDecimal overBorderAmount=gas.subtract(borderAmount);
					//����=���ٽ�������*�ٽ������ۣ�+���ٽ�����*�ٽ������ۣ�
					amount=(overBorderAmount.multiply(overPrice)).add(borderAmount.multiply(gasPrice));
					}
				else{
					//�ٽ�������
					amount=gasPrice.multiply(gas);
				}
			}
			// �ӻ���ȡ�����(�������)
			BigDecimal f_zhye = new BigDecimal(map.get("f_zhye") + "");
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
			Date date1=new Date();
			String date1Str=lastinputdate.substring(0, 10);
			Date inputdate=df.parse(date1Str);
			//�ƻ��·�
//			DateFormat hd=new SimpleDateFormat("yyyy-MM");
//			String dateStr1=handdate.substring(0, 7);
//			Date handDate=hd.parse(dateStr1);
			// ����û��ɷ����������
			if (payment.equals("���")) {
				// ����ɷѼ�¼
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // �û�ID
				sell.put("f_payfeevalid", "��Ч");// �����Ƿ���Ч
				sell.put("f_payfeetype", "���");// �շ�����
				sell.put("lastinputgasnum", lastReading.setScale(0).doubleValue()); // ���ڵ���
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // ���ڵ���
				sell.put("f_totalcost", 0.0); // Ӧ�����
				sell.put("f_grossproceeds", 0.0); // �տ�
				sell.put("f_deliverydate", new Date()); // ��������
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڽ���
				sell.put("f_benqizhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڽ���
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // ��������
				sell.put("f_comtype", "��Ȼ����˾"); // ��˾���ͣ���Ϊ��Ȼ����˾������
				sell.put("f_username", map.get("f_username")); // �û�/��λ����
				sell.put("f_address", map.get("f_address")); // ��ַ
				sell.put("f_districtname", map.get("f_districtname")); // С������
				sell.put("f_idnumber", map.get("f_idnumber")); // ���֤��
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // ����Ʒ��
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // ��������
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ����
				sell.put("f_usertype", map.get("f_usertype")); // �û�����
				sell.put("f_gasproperties", map.get("f_gasproperties")); // ��������
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // ����
				sell.put("f_payment", "�ֽ�"); // ���ʽ
				sell.put("f_sgnetwork", sgnetwork); // ����
				sell.put("f_sgoperator", sgoperator); // �� �� Ա
				sell.put("f_filiale", "�¿���Ȼ����˾"); // �ֹ�˾
				sell.put("f_useful", handid); // ����id
				hibernateTemplate.save("t_sellinggas", sell);

				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// ���γ�������
						"  lastinputdate=? " +
						// ��ǰ���ۼƹ����� ���ݣ� ���ۼƹ�����
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// ������� ��������� �����ʱ��
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.setScale(0).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// ���³����¼
				hql = "update t_handplan set f_state ='�ѳ���',shifoujiaofei='��',"
						+
						// ���γ������� ���ڵ���
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?,lastrecord=? ,oughtamount=? ,oughtfee=? ,f_address=?, f_username=?"
						+ "where f_userid=? and f_state='δ����'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(), address, username,userid });
			}
			// ����û������Ľ�������Ϊ���ʴ���
			else if (payment.equals("���ʴ���")) {
				// ����ɷѼ�¼
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // �û�ID
				sell.put("f_payfeevalid", "��Ч");// �����Ƿ���Ч
				sell.put("f_payfeetype", "���ʴ���");// �շ�����
				sell.put("lastinputgasnum", lastReading.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڵ���
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // ���ڵ���
				sell.put("f_totalcost", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // Ӧ�����
				sell.put("f_grossproceeds", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // �տ�
				sell.put("f_deliverydate", new Date()); // ��������
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڽ���
				sell.put("f_benqizhye", f_zhye.setScale(0).doubleValue()); // ���ڽ���
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // ��������
				sell.put("f_comtype", "��Ȼ����˾"); // ��˾���ͣ���Ϊ��Ȼ����˾������
				sell.put("f_username", map.get("f_username")); // �û�/��λ����
				sell.put("f_address", map.get("f_address")); // ��ַ
				sell.put("f_districtname", map.get("f_districtname")); // С������
				sell.put("f_idnumber", map.get("f_idnumber")); // ���֤��
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // ����Ʒ��
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // ��������
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ����
				sell.put("f_usertype", map.get("f_usertype")); // �û�����
				sell.put("f_gasproperties", map.get("f_gasproperties")); // ��������
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // ����
				sell.put("f_payment", "�ֽ�"); // ���ʽ
				sell.put("f_sgnetwork", sgnetwork); // ����
				sell.put("f_sgoperator", sgoperator); // �� �� Ա
				sell.put("f_filiale", "�¿�ȼ��"); // �ֹ�˾
				sell.put("f_useful", handid); // ����id
				hibernateTemplate.save("t_sellinggas", sell);

				// �����û�����

				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// ���γ�������
						"  lastinputdate=? " +
						// ��ǰ���ۼƹ����� ���ݣ� ���ۼƹ�����
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// ������� ��������� �����ʱ��
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.setScale(0).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// ���³����¼
				hql = "update t_handplan set f_state ='�ѳ���',shifoujiaofei='��',"
						+
						// ���γ������� ���ڵ���
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=? ,lastrecord=? ," 
						+
						// ������ ��������
						"oughtamount=?,     oughtfee=? ,f_address=?, f_username=?, f_endjfdate=?"
						+ "where f_userid=? and f_state='δ����'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2).doubleValue(), address, username , date,userid });
			}
			// ����û��������������������Ƿ�� ��¼
			else if (f_zhye.compareTo(amount)>=0 && items==0) {
				// ����ɷѼ�¼
				Map<String, Object> sell = new HashMap<String, Object>();
				sell.put("f_userid", map.get("f_userid")); // �û�ID
				sell.put("f_payfeevalid", "��Ч");// �����Ƿ���Ч
				sell.put("f_payfeetype", "��潻��");// �շ�����
				sell.put("lastinputgasnum", lastReading.setScale(0).doubleValue()); // ���ڵ���
				sell.put("lastrecord", readingThis.setScale(0).doubleValue()); // ���ڵ���
				sell.put("f_totalcost", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // Ӧ�����
				sell.put("f_grossproceeds", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // �տ�
				//sell.put("f_zhinajin", amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // �տ�
				sell.put("f_deliverydate", new Date()); // ��������
				sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڽ���
				sell.put("f_benqizhye", f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ڽ���
				sell.put("f_gasmeterstyle", map.get("f_gasmeterstyle")); // ��������
				sell.put("f_comtype", "��Ȼ����˾"); // ��˾���ͣ���Ϊ��Ȼ����˾������
				sell.put("f_username", map.get("f_username")); // �û�/��λ����
				sell.put("f_address", map.get("f_address")); // ��ַ
				sell.put("f_districtname", map.get("f_districtname")); // С������
				sell.put("f_idnumber", map.get("f_idnumber")); // ���֤��
				sell.put("f_gaswatchbrand", map.get("f_gaswatchbrand")); // ����Ʒ��
				sell.put("f_gaspricetype", map.get("f_gaspricetype")); // ��������
				sell.put("f_gasprice", gasPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ����
				sell.put("f_usertype", map.get("f_usertype")); // �û�����
				sell.put("f_gasproperties", map.get("f_gasproperties")); // ��������
				sell.put("f_pregas", gas.setScale(0).doubleValue()); // ����
				sell.put("f_payment", "�ֽ�"); // ���ʽ
				sell.put("f_sgnetwork", sgnetwork); // ����
				sell.put("f_sgoperator", sgoperator); // �� �� Ա
				sell.put("f_filiale", "�¿���Ȼ����˾"); // �ֹ�˾
				sell.put("f_useful", handid); // ����id
				hibernateTemplate.save("t_sellinggas", sell);

				// �����û�����

				// ���� ���ڵ���
				hql = "update t_userfiles set f_zhye=?,lastinputgasnum=?," +
				// ���γ�������
						"  lastinputdate=? " +
						// ��ǰ���ۼƹ����� ���ݣ� ���ۼƹ�����
						// "f_metergasnums= f_metergasnums + ?, f_cumulativepurchase=f_cumulativepurchase+? ,"
						// ������� ��������� �����ʱ��
						// "f_finallybought= ?, f_finabuygasdate=?, f_finabuygastime=? "
						
						"where f_userid=?";

				hibernateTemplate.bulkUpdate(hql, new Object[] {
						f_zhye.subtract(amount).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),
						readingThis.setScale(0).doubleValue(), lastinputDate,
						userid });

				// ���³����¼
				hql = "update t_handplan set f_state ='�ѳ���',shifoujiaofei='��',"
						+
						// ���γ������� ���ڵ���
						"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=? ,lastrecord=? ," 
						+
						// ������ ��������
						"oughtamount=?,     oughtfee=? ,f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?"
						+ "where f_userid=? and f_state='δ����'";
				hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
						zerenbumen, menzhan, inputtor,
						readingThis.setScale(0).doubleValue(),
						gas.setScale(0).doubleValue(),
						amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),inputdate,sgnetwork,sgoperator ,address, username , userid });
			} else {
				// �����û�����
				hql = "update t_userfiles " +
				// ���γ������ ���γ�������
						"set lastinputgasnum=? ,  lastinputdate=?  where f_userid=?";
				hibernateTemplate.bulkUpdate(hql, new Object[] {
						readingThis.setScale(0).doubleValue(), lastinputDate, userid });

				// Ƿ��,���³����¼��״̬f_state���������ڡ����γ������
				hql = "update t_handplan set f_state ='�ѳ���', shifoujiaofei='��',"
					+
					// ���γ�������  ���β���  ��վ  ����Ա  ���ڵ���
					"lastinputdate=?,   f_zerenbumen=?, f_menzhan=?, f_inputtor=?, lastrecord=? ," 
					+
					// ������     ���ѽ�ֹ���� ��������
					"oughtamount=?,  f_endjfdate=? , oughtfee=?, f_inputdate=?,f_network=?,f_operator=? ,f_address=?, f_username=?"
					+ "where f_userid=? and f_state='δ����'";
			hibernateTemplate.bulkUpdate(hql, new Object[] { lastinputDate,
					zerenbumen, menzhan, inputtor,
					readingThis.setScale(0).doubleValue(),
					gas.setScale(0).doubleValue(),date,
					amount.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(),inputdate,sgnetwork,sgoperator, address, username, userid });
			
			return "";
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new WebApplicationException(401);
		}
		return null;
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
	@Path("record/batch/{handdate}/{sgnetwork}/{sgoperator}/{lastinputdate}")
	@POST
	public String RecordInputForMore(String data,
			@PathParam("sgnetwork") String sgnetwork,
			@PathParam("sgoperator") String sgoperator,
			@PathParam("lastinputdate") String lastinputdate,
			@PathParam("handdatete") String handdate)  {
		try {
			// ȡ����������
			JSONArray rows = new JSONArray(data);
			// ��ÿһ�����ݣ����õ����������ݴ������
			for (int i = 0; i < rows.length(); i++) {
				JSONObject row = rows.getJSONObject(i);
				String userid = row.getString("userid");
				double reading = row.getDouble("reading");
				
				BigDecimal readingThis = new BigDecimal(reading + "");
				RecordInputForOne(userid, reading, sgnetwork, sgoperator,lastinputdate,handdate);
			}
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