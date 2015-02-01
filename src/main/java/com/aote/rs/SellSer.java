package com.aote.rs;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Path("sell")
@Component
public class SellSer {
	static Logger log = Logger.getLogger(SellSer.class);
	@Autowired
	private HibernateTemplate hibernateTemplate;

	// ����sell������������
	@GET
	@Path("{userid}/{money}/{zhinajin}/{payment}/{opid}")
	public String txSell(@PathParam("userid") String userid,
			@PathParam("money") double dMoney,
			@PathParam("zhinajin") double dZhinajin,
			@PathParam("payment") String payment, @PathParam("opid") String opid) {
		try {
			// ���ҵ�½�û�,��ȡ��½����,����Ա
			Map<String, Object> loginUser = this.findUser(opid);
			if (loginUser == null) {
				log.error("����ɷѴ���ʱδ�ҵ���½�û�,��½id" + opid);
				throw new WebApplicationException(401);
			}
			String sgnetwork = loginUser.get("f_parentname").toString();
			String sgoperator = loginUser.get("name").toString();
			String fengongsi = loginUser.get("f_fengongsi").toString();
			String fengongnum = loginUser.get("f_fengongsinum").toString();

			// �����û�����ҵ��û������е���Ϣ,�Լ������¼
			String sql = " select u.f_zhye f_zhye, u.f_username f_username,u.f_cardid f_cardid, u.f_address f_address,u.f_districtname f_districtname,u.f_cusDom f_cusDom,u.f_cusDy f_cusDy,u.f_beginfee f_beginfee, u.f_metergasnums f_metergasnums, u.f_cumulativepurchase f_cumulativepurchase,"
					+ "u.f_idnumber f_idnumber, u.f_gaspricetype f_gaspricetype, u.f_gasprice f_gasprice, u.f_usertype f_usertype,"
					+ "u.f_gasproperties f_gasproperties, u.f_userid f_userid, h.id handid, h.oughtamount oughtamount, h.lastinputgasnum lastinputgasnum,"
					+ "h.lastrecord lastrecord, h.shifoujiaofei shifoujiaofei, h.oughtfee oughtfee from t_userfiles u "
					+ "left join (select * from t_handplan where f_state = '�ѳ���' and shifoujiaofei = '��') h on u.f_userid = h.f_userid where u.f_userid = '"
					+ userid
					+ "' "
					+ "order by u.f_userid, h.lastinputdate, h.lastinputgasnum";
			HibernateSQLCall sqlCall = new HibernateSQLCall(sql);
			List<Map<String, Object>> list = this.hibernateTemplate
					.executeFind(sqlCall);
			// ���տ�תΪBigDecimal
			BigDecimal money = new BigDecimal(dMoney + "");
			BigDecimal zhinajin = new BigDecimal(dZhinajin + "");
			// ȡ����һ����¼���Ա���û�������ȡ����
			Map<String, Object> userinfo = (Map<String, Object>) list.get(0);
			// ���û�������ȡ���ۼƹ�����
			BigDecimal f_metergasnums = new BigDecimal(userinfo.get(
					"f_metergasnums").toString());
			BigDecimal f_cumulativepurchase = new BigDecimal(userinfo.get(
					"f_cumulativepurchase").toString());
			// ��¼�ϴι�����������ʱʹ�ã�
			BigDecimal oldf_metergasnums = new BigDecimal(userinfo.get(
					"f_metergasnums").toString());// �ɵı�ǰ�ۼƹ�����
			BigDecimal oldf_cumulativepurchase = new BigDecimal(userinfo.get(
					"f_cumulativepurchase").toString());// �ɵ����ۼƹ�����

			// ���û�������ȡ�����
			BigDecimal f_zhye = new BigDecimal(userinfo.get("f_zhye")
					.toString());
			// �����+ʵ���շѽ��-���ɽ� �ٺ�Ӧ�����Ƚϣ��ж�δ���ѵĳ����¼�Ƿ��ܹ�����
			BigDecimal total = f_zhye.add(money).subtract(zhinajin);
			// �ܵ�����ָ��
			BigDecimal lastnum = new BigDecimal("0");
			// ������
			BigDecimal gasSum = new BigDecimal("0");
			// ������
			BigDecimal feeSum = new BigDecimal("0");
			// �����¼id
			String handIds = "";
			for (Map<String, Object> map : list) {

				// ȡ��Ӧ�����
				String h = (map.get("oughtfee") + "");
				if (h.equals("null")) {
					h = "0.0";
				} else {
				}
				BigDecimal oughtfee = new BigDecimal(h);
				// ��ǰ�û�ʵ�ʽɷѹ�������۳������Ѽ�¼��Ϊ�ѽ�
				int equals = total.compareTo(oughtfee);// �ж�total��oughtfee�Ĵ�С
				if (equals >= 0) {
					// �۷ѣ��������������
					total = total.subtract(oughtfee);
					// ���ѳɹ�������ָ�����
					String lastinputgasnum1 = (map.get("lastinputgasnum") + "");
					if (lastinputgasnum1.equals("null"))
						lastinputgasnum1 = "0.0";
					BigDecimal lastinputgasnum = new BigDecimal(
							lastinputgasnum1);
					lastnum = lastnum.add(lastinputgasnum);

					// �������
					String oughtamount1 = (map.get("oughtamount") + "");
					if (oughtamount1.equals("null"))
						oughtamount1 = "0.0";
					BigDecimal gas = new BigDecimal(oughtamount1);
					gasSum = gasSum.add(gas);
					// �ۼƹ�����
					f_metergasnums = f_metergasnums.add(gasSum);
					f_cumulativepurchase = f_cumulativepurchase.add(gasSum);
					// �������
					feeSum = feeSum.add(oughtfee);
					// ��ȡ�����¼ID
					Integer handId1 = (Integer) map.get("handid");
					if (handId1 == null)
						handId1 = 0;
					int handId = handId1;
					// �����¼Ids
					handIds = add(handIds, handId + "");
					// ���³����¼
					System.out.println(handIds + "��update��ʼ");
					String updateHandplan = "update t_handplan set shifoujiaofei='��' where id="
							+ handId;
					hibernateTemplate.bulkUpdate(updateHandplan);
				}
			}
			// �����û�����
			String updateUserinfo = "update t_userfiles set f_zhye=" + total
					+ " ,f_metergasnums=" + f_metergasnums
					+ " ,f_cumulativepurchase=" + f_cumulativepurchase
					+ " where f_userid='" + userid + "'";
			hibernateTemplate.bulkUpdate(updateUserinfo);
			// �������Ѽ�¼
			Map<String, Object> sell = new HashMap<String, Object>();

			sell.put("f_userid", userid); // ����id
			sell.put("lastinputgasnum", lastnum.setScale(1,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // ����ָ��
			sell.put("lastrecord", lastnum.add(gasSum).setScale(1,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // ����ָ��
			sell.put("f_totalcost", zhinajin.add(feeSum).subtract(f_zhye)
					.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // Ӧ�����
			sell.put("f_grossproceeds", money.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // �տ�
			sell.put("f_zhinajin", zhinajin.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ɽ�

			Date now = new Date();
			sell.put("f_deliverydate", now); // ��������
			sell.put("f_deliverytime", now); // ����ʱ��

			sell.put("f_zhye", f_zhye.setScale(2, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // ���ڽ���
			sell.put("f_benqizhye", total.setScale(2, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // ���ڽ���
			sell.put("f_beginfee", userinfo.get("f_beginfee")); // ά�ܷ�
			sell.put("f_premetergasnums", oldf_metergasnums.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // ���ϴ��ۼƹ�����
			sell.put("f_upbuynum", oldf_cumulativepurchase.setScale(2,
					BigDecimal.ROUND_HALF_UP).doubleValue()); // �ϴ����ۼƹ�����
			sell.put("f_gasmeterstyle", "����"); // ��������
			sell.put("f_comtype", "��Ȼ����˾"); // ��˾���ͣ���Ϊ��Ȼ����˾������
			sell.put("f_username", userinfo.get("f_username")); // �û�/��λ����
			sell.put("f_address", userinfo.get("f_address")); // ��ַ
			sell.put("f_districtname", userinfo.get("f_districtname")); // ��ַ
			sell.put("f_cusDom", userinfo.get("f_cusDom")); // ��ַ
			sell.put("f_cusDy", userinfo.get("f_cusDy")); // ��ַ
			sell.put("f_idnumber", userinfo.get("f_idnumber")); // ���֤��
			sell.put("f_gaswatchbrand", "����"); // ����Ʒ��
			sell.put("f_gaspricetype", userinfo.get("f_gaspricetype")); // ��������
			sell.put("f_gasprice", userinfo.get("f_gasprice")); // ����
			sell.put("f_usertype", userinfo.get("f_usertype")); // �û�����
			sell.put("f_gasproperties", userinfo.get("f_gasproperties"));// ��������
			//�����У���������Ϊ�洢���Ӻţ�����������Ϣ�ֶ�
			if(userinfo.containsKey("f_cardid")&& userinfo.get("f_cardid")!=null)
			{
			  String kh = userinfo.get("f_cardid").toString();
			  sell.put("f_cardid", kh);
			}
				
			sell.put("f_pregas", gasSum.setScale(1, BigDecimal.ROUND_HALF_UP)
					.doubleValue()); // ����
			sell.put("f_preamount", feeSum
					.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()); // ����
			sell.put("f_payment", payment); // ���ʽ
			sell.put("f_paytype", "�ֽ�"); // �������ͣ����д���/�ֽ�
			sell.put("f_sgnetwork", sgnetwork); // ����
			sell.put("f_sgoperator", sgoperator); // �� �� Ա
			sell.put("f_filiale", fengongsi); // �ֹ�˾
			sell.put("f_fengongsinum", fengongnum); // �ֹ�˾���
			sell.put("f_payfeetype", "�����շ�"); // ��������
			sell.put("f_payfeevalid", "��Ч"); // ������Ч����
			sell.put("f_useful", handIds); // �����¼id
			int sellId = (Integer) hibernateTemplate.save("t_sellinggas", sell);
			// ��ʽ����������
			SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String result = "{id:" + sellId + ", f_deliverydate:'"
					+ f2.format(now) + "'}";
			// ���³����¼sellid
			String updateHandplan = "update t_handplan set f_sellid =" + sellId
					+ " where id in (" + handIds + ")";
			hibernateTemplate.bulkUpdate(updateHandplan);
			return "";
		} catch (Exception ex) {
			// �Ǽ��쳣��Ϣ
			log.error(ex.getMessage());
			throw new WebApplicationException(401);
		}
	}

	// ���ҵ�½�û�
	private Map<String,Object> findUser(String loginId) {
		String findUser = "from t_user where id='" + loginId + "'";
		List<Object> userList = this.hibernateTemplate.find(findUser);
		if (userList.size() != 1) {
			return null;
		}
		return (Map<String,Object>)userList.get(0);
	}

	// ִ��sql��ѯ
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

	// ���ַ�����Ӷ��ŷָ�������
	private String add(String source, String str) {
		if (source.equals("")) {
			return source + str;
		} else {
			return source + "," + str;
		}
	}
}
