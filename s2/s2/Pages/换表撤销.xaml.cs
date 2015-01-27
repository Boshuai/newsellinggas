using Com.Aote.Behaviors;
using Com.Aote.ObjectTools;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;

namespace Com.Aote.Pages
{
    public partial class 换表撤销 : UserControl
    {
        // search对象
        SearchObject userSearch = new SearchObject();

        // 选中的表格对象
        PagedList userList = new PagedList();        
        public 换表撤销()
        {
            InitializeComponent();

            // 给界面上的search部分，设置数据
            pipelinesearch.DataContext = userSearch;

            // 给两个表格对象挂接数据
            pipelines.ItemsSource = userList;
            userList.DataLoaded += userList_DataLoaded;
        }

        int pageIndex = 0;
        private void searchbutton_Click(object sender, RoutedEventArgs e)
        {
            ui_userBusy.IsBusy = true;
            userSearch.Search();

            // 生成sql语句
            string sql = "select * from t_changmeter where " + userSearch.Condition + " and f_payfeevalid!='无效' order by id";
            userList.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            userList.LoadOnPathChanged = false;
            userList.Path = "sql";
            userList.SumHQL = "select * from t_changmeter where " + userSearch.Condition + " and f_payfeevalid!='无效'";
            userList.HQL = sql;
            userList.PageSize = ui_pager.PageSize;
            userList.SumNames = "id";
            userList.PageIndex = pageIndex;
            userList.Load();
        }

        private void userList_DataLoaded(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            ui_userBusy.IsBusy = false;
        }

        private void pipelines_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            pipeline.DataContext = pipelines.SelectedItem;
        }

        private void ui_pager_PageIndexChanged(object sender, EventArgs e)
        {
            userList.PageIndex=ui_pager.PageIndex;
        }

        private void onlySave_Click(object sender, RoutedEventArgs e)
        {
            ui_meterBusy.IsBusy = true;
            string sql = "update t_changmeter set f_cancelnote='" + ui_f_cancelnote.Text + "',f_canceldate='" + ui_f_canceldate.SelectedDate + "',f_cxoperation='"+ ui_f_cxoperation.Text +"',f_payfeevalid='无效' where id=" + ui_id.Text;
            HQLAction action = new HQLAction();
            action.HQL = sql;
            action.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            action.Name = "t_changmeter";
            action.Completed += action_Completed;
            action.Invoke();
        }

        private void action_Completed(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            string sql = "update t_userfiles set f_initcardsellgas = null,f_gaswatchbrand='"
            + ui_gaswatchbrand.Text + "',f_gasmetermanufacturers='" + ui_gasmetermanufacturers .Text+
            "',f_metertype='" + ui_metertype.Text + "',f_gaswatchbrand='" + ui_gaswatchbrand.Text +
            "' where id=" + ui_id.Text;
            HQLAction action1 = new HQLAction();
            action1.HQL = sql;
            action1.WebClientInfo = Application.Current.Resources["dbclient"] as WebClientInfo;
            action1.Name = "t_changmeter";
            action1.Completed += action_Completed1;
            action1.Invoke();
        }
         private void action_Completed1(object sender, System.ComponentModel.AsyncCompletedEventArgs e)
        {
            userList.IsOld = true;
            ui_meterBusy.IsBusy = false;
        }
    }
}
