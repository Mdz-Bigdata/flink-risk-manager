import React from 'react';
import { Card, Statistic } from 'antd';
import { useStyles } from './StatCard.styles';

interface StatCardProps {
  title: string;
  value: number | string;
  icon?: React.ReactNode;
  valueStyle?: React.CSSProperties;
  trend?: 'up' | 'down' | 'flat';
  loading?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, valueStyle, trend, loading }) => {
  const { styles } = useStyles();
  return (
    <Card className={styles.card} loading={loading} bordered={false}>
      <div className={styles.content}>
        <div className={styles.info}>
          <Statistic title={title} value={value} valueStyle={valueStyle} />
        </div>
        {icon && <div className={styles.icon}>{icon}</div>}
      </div>
    </Card>
  );
};

export default StatCard;
